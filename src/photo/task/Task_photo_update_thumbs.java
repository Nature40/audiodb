package photo.task;

import photo.PhotoDB2.Interrupter;
import task.Cancelable;
import task.Description;
import task.Param;
import task.Role;
import task.Tag;
import task.Task;
import task.Descriptor.Param.Type;

@Tag("photo")
@Description("Create missing image thumbnails. (Multiple update tasks do have no effect.)")
@Param(name = "run", type = Type.BOOLEAN, preset = "TRUE", description = "Run update task, if 'false' do not run update task and currently running update tasks will be stopped.")
@Cancelable
@Role("admin")
public class Task_photo_update_thumbs extends Task {

	@Override
	public void run() throws InterruptedException {
		if(this.ctx.getParamBoolean("run")) {
			ctx.broker.photodb2().updateThumbs();
			while(true) {
				if(this.isSoftCanceled()) {
					Interrupter interrupter = ctx.broker.photodb2().getInterrupterUpdateThumbs();
					if(interrupter != null) {
						interrupter.interrupted = true;
						break;
					}
				}
				Interrupter interrupter = ctx.broker.photodb2().getInterrupterUpdateThumbs();
				if(interrupter == null) {
					break;
				} else {
					if(interrupter.interrupted) {
						break;
					}
				}
				Thread.sleep(1000);
			}
		} else {
			Interrupter interrupter = ctx.broker.photodb2().getInterrupterUpdateThumbs();
			if(interrupter != null) {
				interrupter.interrupted = true;
			}
		}
	}
}
