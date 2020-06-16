<template>
<v-container>
  <div class="send-overlay" v-show="sendMessage !== undefined">
    {{sendMessage}}
  </div>
  <div style="position: absolute; left: 100px;">
    {{currentTimeAudio.toFixed(3)}} / {{duration.toFixed(3)}} 
  </div>
  <div style="position: absolute; right: 0px;">
    <player-settings />
    <label-definitions @onDialog="labelDefinitionsDialogOpen = $event" />
    <div class="send-error" v-show="sendMessageError !== undefined">
      <v-icon color="red">warning</v-icon>
      {{sendMessageError}}
      <br>{{sendMessageErrorReason}}
    </div>  
  </div>
  <div v-show="!imageLoaded" style="position: absolute; background-color: lightgrey; padding: 100px; margin: 100px; margin-left: 400px;; border-style: solid;">
    <h1>loading ...</h1>
    <ring-loader loading="loading" color="#000000" size="50px" />
  </div>
  <v-layout text-xs-center wrap>
    <v-flex xs12 mb-5 >
      <audio id="player" :src="apiBase + 'samples/'+ sample.id + '/data'" type="audio/wav" controls preload="auto">
      </audio>
      <br>
      <div style="display: inline-block; ">
              <v-slider
        v-model="currentTimeUser"
        :min="0"
        :max="duration"
        :step="0"
        style="margin-bottom: -25px; margin-top: -10px;"
        :style="{ width: canvasWidth + 'px' }"        
      ></v-slider>
      <canvas id="canvas" :class="{ 'semi_transparent': (!imageLoaded) }" :width="canvasWidth" :height="canvasHeight" style="background-color: #d1d1d1;" @mousedown="startDrag" @mousemove="dragMove" class="spectrogram" />
      <img id="image" :src="spectrumUrl" style="width: 1800px; display: none;" />
      <br>

        <multiselect 
          v-model="selectedLabelNames" 
          :options="mergedLabelNames" 
          label="name" 
          style="max-width: 300px; display: inline-block; vertical-align: top;" 
          placeholder="Search or add label"
          tagPlaceholder="Press ENTER to add this as new label" 
          :allowEmpty="true"
          :taggable="true"
          :multiple="true"
          @tag="addLabelName"
          track-by="name"
          @open="selectLabel = true"
          @close="selectLabel = false"
          :limit="1"
          :limitText="count => `and ${count} more`"
          :class="{ 'hide': (labelEndTime === undefined) }"
        />

        <div style="position: relative; display: inline-block; vertical-align: top;">
          <v-btn small round color="primary" class="hide" >start label</v-btn>
          <v-btn @click="onLabelStart" small round color="primary" v-show="labelStartTime === undefined" title="create new label that starts at current audio position" style="position: absolute; top: 0px; left: 0px;">start label</v-btn>
          <v-btn @click="onLabelEnd" :disabled="labelStartTime === undefined || currentTimeAudio === undefined || labelStartTime === currentTimeAudio" small round color="primary" v-show="labelStartTime !== undefined && labelEndTime === undefined" title="end current label at current audio position"  style="position: absolute; top: 0px; left: 0px;">end label</v-btn>
          <v-btn @click="onLabelSave" small round color="primary" v-show="labelEndTime !== undefined" title="store current label" style="position: absolute; top: 0px; left: 0px;">save</v-btn>
        </div>
        <v-btn @click="onLabelDiscard" small round color="primary" :class="{ 'hide': (labelEndTime === undefined) }" title="remove current label" style="vertical-align: top;">discard</v-btn>
        <v-text-field v-model="labelComment" placeholder="comment" class="input-comment" :class="{ 'hide': (labelEndTime === undefined) }" style="vertical-align: top;"></v-text-field>        
      </div>
      press <b>[SPACE]</b> key to <b>play</b> / <b>pause</b> audio
      <br>
      press <b>[ENTER]</b> key to <b :class="{active: labelStartTime === undefined}">mark start</b> / <b :class="{active: labelStartTime !== undefined && labelEndTime === undefined}">mark end</b> / <b :class="{active: labelStartTime !== undefined && labelEndTime !== undefined && !selectLabel}">save label</b>
      <br>
      press <b>[LEFT/RIGTH ARROW]</b> key to move <b>back/forward</b> in time
      <br>
      on spectrogram hold <b>[left mouse button]</b> and <b>[move mouse left/right]</b> to move in time <b></b>


      <br>
      <br>
      <table class="table-labels">
      <tr>
        <th>remove</th>
        <th>start</th>
        <th>end</th>
        <th>label</th>
        <th>move to</th>
        <th>comment</th>
      </tr>
      <tbody>
      <tr v-for="(label, index) in labels" :key="index">
        <td><v-btn icon title="remove label"><v-icon @click="onLabelRemove(index)">delete_forever</v-icon></v-btn></td>
        <td>{{label.start.toFixed(3)}}</td>
        <td>{{label.end.toFixed(3)}}</td>
        <td><span v-for="labelName in label.labels" :key="labelName" class="label-name">{{labelName}}</span></td>
        <td><v-btn icon title="move to label start"><v-icon @click="currentTimeUser = label.start">redo</v-icon></v-btn></td>
        <td>{{label.comment}}</td>
      </tr>
      <tr v-if="labels.length === 0">
        ---- (no labels) ----
      </tr>
      </tbody>
      </table>
    </v-flex>
  </v-layout>
   
</v-container>
</template>

<script>

import axios from 'axios'
import { mapState, mapActions } from 'vuex'

import playerSettings from './player-settings'
import labelDefinitions from './label-definitions'

var refPlayer = undefined;

export default {
props: ['sample'],
components: {
  playerSettings,
  labelDefinitions,
},
data: () => ({
  currentTimeAudio: 0,      
  currentTimeUser: 0,
  duration: 0,
  frequencyData: [],
  sampleRate: 10000,
  canvasWidth: 1024,
  canvasHeight: 512,
  imageLoaded: false,
  dragStartX: undefined,
  secondsPerColumn: undefined,
  labelStartTime: undefined,
  labelEndTime: undefined,
  labelComment: undefined,
  selectedLabelNames: [],
  customLabelNames: [],
  labels: [],
  selectLabel: false,
  labelDefinitionsDialogOpen: false,
  sendMessage: undefined,
  sendMessageError: undefined,
  sendMessageErrorReason: undefined,
}),
computed: {
  ...mapState({
    apiBase: 'apiBase',
    label_definitions: state => state.label_definitions === undefined ? undefined : state.label_definitions.data,
    threshold: state => state.settings.player_spectrum_threshold,
  }),
  mergedLabelNames() {
    return this.label_definitions === undefined ? this.customLabelNames : this.label_definitions.concat(this.customLabelNames);
  },
  shortcutsBlocked() {
    return this.labelDefinitionsDialogOpen;
  },
  spectrumUrl() {
    return this.apiBase + 'samples/' + this.sample.id + '/spectrum' + '?cutoff=' + this.canvasHeight + "&threshold=" + this.threshold;
  },
},  
methods: {
  ...mapActions({
    label_definitions_init: 'label_definitions/init',
    label_definitions_refresh: 'label_definitions/refresh',
  }),  
  startDrag(e) {        
    this.dragStartX = e.pageX;
  },
  dragMove(e) {
    if(this.dragStartX !== undefined) {
      if(e.buttons == 1) { // left mouse button
        var offsetX = e.pageX - this.dragStartX;
        this.currentTimeUser -= offsetX * this.secondsPerColumn;
        this.dragStartX = e.pageX;
      } else {
        this.dragStartX = undefined;
      }
    }
  },
  onLabelStart() {
    this.labelStartTime = this.currentTimeAudio;
  },
  onLabelEnd() {
    this.labelEndTime = this.currentTimeAudio;
  },
  onLabelDiscard() {
    this.labelStartTime = undefined;
    this.labelEndTime = undefined;
  },
  onLabelSave() {
    var names = this.selectedLabelNames.map(l=>l.name);
    var label = {start: this.labelStartTime, end: this.labelEndTime, labels: names, comment: this.labelComment};
    //this.labels.push(label);
    //this.labelStartTime = undefined;
    //this.labelEndTime = undefined;
    this.postAddLabel(label);
  },
  addLabelName(labelText) {
    var x = {name: labelText};
    this.customLabelNames.push(x);
    this.selectedLabelNames.push(x);
  },
  postAddLabel(label) {
    this.sendMessage = "send: add label";
    this.sendMessageError = undefined;
    axios.post(this.apiBase + 'samples' + '/' + this.sample.id + '/' + 'labels', {actions: [{action: "add_label", label: label}]})
    .then((response) => {
      this.sendMessage = undefined;
      this.sendMessageError = undefined;
      this.labelStartTime = undefined;
      this.labelEndTime = undefined;
      this.labels = response.data.labels;
    })
    .catch(() => {
      this.sendMessage = undefined;
      this.sendMessageError = "could not send: add label. You may tray again.";
    });
  },
  refreshLabels() {
    console.log("refreshLabels");
    axios.get(this.apiBase + 'samples' + '/' + this.sample.id + '/' + 'labels')
    .then(response => {
      this.labels = response.data.labels;
    })
    .catch(() => {
      this.labels = [];
    });
  },
  onLabelRemove(index) {
    //this.labels.splice(index, 1);
    this.postRemoveLabel(this.labels[index]);
  },
  postRemoveLabel(label) {
    this.sendMessage = "send: remove label";
    this.sendMessageError = undefined;
    axios.post(this.apiBase + 'samples' + '/' + this.sample.id + '/' + 'labels', {actions: [{action: "remove_label", label: label}]})
    .then((response) => {
      this.sendMessage = undefined;
      this.sendMessageError = undefined;
      this.labelStartTime = undefined;
      this.labelEndTime = undefined;
      this.labels = response.data.labels;
    })
    .catch((error) => {
      this.sendMessage = undefined;
      console.log(error.response);
      this.sendMessageError = "could not send: remove label. You may tray again.";
      this.sendMessageErrorReason = error === undefined ? "unkown reason" : error.response === undefined ? "unkown reason" : error.response.data === undefined ? "unkown reason" : error.response.data; 
    });
  },
  onWindowResize() {
    this.canvasWidth = document.body.clientWidth - 200;
  }  
},
watch: {
  currentTimeUser() {
    if(this.currentTimeUser !== this.currentTimeAudio) {
      //console.log("user change");
      var audio = document.getElementById('player');
      //console.log(audio);
      audio.currentTime = this.currentTimeUser;
      //console.log("set currentTimeUser " + this.currentTimeUser);
    }
  },
  sample() {
    console.log("watch sample");
    //this.imageLoaded = false;
    this.refreshLabels();
  },
},
created() {
  window.addEventListener("resize", this.onWindowResize.bind(this));
},
destroyed() {
  window.removeEventListener("resize", this.onWindowResize.bind(this));
},
mounted() {
  var self = this;
  this.onWindowResize();
  this.refreshLabels();
  this.label_definitions_init();
  refPlayer = this;
  var audio = document.getElementById('player');

  const canvas = document.getElementById('canvas');
  const ctx = canvas.getContext('2d');
  const image = document.getElementById('image');
  audio.addEventListener("progress", function() {
    console.log("progress"); 
    }, true
  );
  audio.addEventListener("play", function() {
    console.log("play");
    }, true
  );
  audio.addEventListener("playing", function() {
    console.log("playing"); 
    }, true
  );
  audio.addEventListener("timeupdate", function() {
    console.log("timeupdate"); 
    }, true
  );
  setInterval(() => {
    self.imageLoaded = image.complete;
    var currentTime = audio.currentTime;
    self.currentTimeAudio = currentTime;
    self.currentTimeUser = currentTime;
    self.duration = audio.duration;
    var canvasNowColumn = this.canvasWidth / 2;
    var columnsPerSecond = image.naturalWidth / audio.duration;
    self.secondsPerColumn = audio.duration / image.naturalWidth;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    //ctx.drawImage(image, canvasNowColumn - (currentTime*collumnsPerSecond), 0);
    //ctx.drawImage(image, (currentTime*collumnsPerSecond) - canvasNowColumn, 0 , 1000, 256, 0, 0, 1000, 256);
    var sx = (currentTime*columnsPerSecond) - canvasNowColumn;
    var sy = 0;
    var sWidth = self.canvasWidth;
    var sHeight = self.canvasHeight;
    var dx = 0;
    var dy = 0;
    var dWidth = sWidth;
    var dHeight = sHeight;
    ctx.drawImage(image, sx, sy, sWidth, sHeight, dx, dy, dWidth, dHeight);

    if(self.labelStartTime !== undefined) {
      var labelXStart = canvasNowColumn - (currentTime - self.labelStartTime)*columnsPerSecond;
      var endTime = self.labelEndTime === undefined ? currentTime : self.labelEndTime;
      var labelXEnd = canvasNowColumn - (currentTime - endTime)*columnsPerSecond; 
      ctx.fillStyle = "rgba(255,255,0,0.5)";
      ctx.fillRect(labelXStart, 0, labelXEnd - labelXStart, self.canvasHeight);
    }

    ctx.beginPath();
    for (var i = 0; i < self.labels.length; i++) {
      var label = self.labels[i];
      var xStart = canvasNowColumn - (currentTime - label.start)*columnsPerSecond;
      var xEnd = canvasNowColumn - (currentTime - label.end)*columnsPerSecond;
      //console.log("range " + xStart + "  " + xEnd + JSON.stringify(label) +" OK "); 
      ctx.moveTo(xStart, self.canvasHeight - 3);
      ctx.lineTo(xEnd, self.canvasHeight - 3);
    }
    ctx.lineWidth = 5;
    ctx.strokeStyle = 'rgb(0,255,0)';
    ctx.stroke();  

    ctx.beginPath();     
    ctx.moveTo(canvasNowColumn, 0); 
    ctx.lineTo(canvasNowColumn, self.canvasHeight); 
    ctx.lineWidth = 1;
    ctx.strokeStyle = 'red';
    ctx.stroke();
  }, 40);

  var someKeysAreDown = false;
  
  window.onkeyup = () => {
    someKeysAreDown = false;
  };

  window.onkeydown = (e) => {
    if(someKeysAreDown) {
      return;
    }
    someKeysAreDown = true;
    if(refPlayer.shortcutsBlocked) {
      return;
    }
    console.log(e.key);
    if(e.key === ' ') {
      if(audio.paused) {
        audio.play();
      } else {
        audio.pause();
      }
      if (e.target == document.body) {
        e.preventDefault();
      }          
    } else if (e.key === 'Enter' && refPlayer !== undefined) {
      if(refPlayer.labelStartTime === undefined) {
        refPlayer.onLabelStart();
      } else if(refPlayer.labelStartTime !== undefined && refPlayer.labelEndTime === undefined) {
        if(refPlayer.labelStartTime !== refPlayer.currentTimeAudio) {
          refPlayer.onLabelEnd();
        }
      } else if(refPlayer.labelStartTime !== undefined && refPlayer.labelEndTime !== undefined && !refPlayer.selectLabel) {
          refPlayer.onLabelSave();
      }
    } else if (e.key === 'ArrowLeft' && refPlayer !== undefined) {
      this.currentTimeUser -= 1;
    } else if (e.key === 'ArrowRight' && refPlayer !== undefined) {
      this.currentTimeUser += 1;
    }
  };
},    
}
</script>

<style scoped>

.hide {
  visibility: hidden;
}

.semi_transparent {
  opacity: 0.5;
  filter: blur(10px);
}

.table-labels {
  border-style: solid;
  border-width: 2px;
  border-color: #59544d7d;
  border-radius: 6px;
}

.table-labels tr td {
  padding-left: 5px;
  padding-right: 5px;
  padding-top: 2px;
  padding-bottom: 2px;
}

.table-labels tr:has(th) {
  background-color: black;
}

.table-labels th {
  text-align: center;
  background-color: #b4b4b4;
  padding-top: 1px;
  padding-left: 5px;
  padding-right: 5px;
  padding-bottom: 1px;  
}

.table-labels td:nth-child(n+1):nth-child(-n+3) {
  font-family: monospace, sans-serif;
  text-align: right;
}

.table-labels tbody tr:nth-child(odd) td {
  background-color: #ececec;
}

.table-labels tbody tr:nth-child(even) td {
  background-color: #dbdbdb;
}

.label-name {
  background-color: #c8b69887;
  padding: 3px;
  margin-right: 5px;
  border-style: solid;
  border-width: 1px;
  border-color: #a5957bd9;
  border-radius: 6px;
  box-shadow: 5px 5px 7px #443c3145;
}

.active {
  background-color: #669eb030;
}

.spectrogram {
  border-style: solid;
  border-width: 1px;
  border-color: #a5957bd9;
  border-radius: 6px;
  box-shadow: 0px 0px 15px #443c319c;
}

audio {
  border-style: solid;
  border-width: 1px;
  border-color: #a5957bb5;
  border-radius: 6px;
}

.input-comment {
  width: 100px;
  display: inline-block;
  margin-right: 10px;
}

.send-overlay {
  position: absolute;
  width: 100%;
  height: 100%;
  z-index: 1;
  background-color: #5e5e5e30;
}

.send-error {
  color: #ee2727e0;
  background-color: #73717138;
  border-color: #ff0e008c;
  border-style: solid;
  padding: 5px;
}

</style>

<style>

html {
  overflow: auto;
}

</style>
