const CompressionWebpackPlugin = require("compression-webpack-plugin");

module.exports = {
    publicPath: '', // use relative paths
    outputDir: '../webcontent/app',
    filenameHashing: false,
    devServer: {
		host: '127.0.0.1',
		port: 8081,
		https: false,
		headers: {
			"Access-Control-Allow-Origin": "*",
		},
		proxy: {
			'': {
				target: 'http://127.0.0.1:8080',
				ws: false,	  
			},
		}
    },
    productionSourceMap: false,
    configureWebpack: {
		performance: {  // remove warning of large size
			maxEntrypointSize: 10000000,
			maxAssetSize: 10000000,
		},		
		plugins: [
		/*new CompressionWebpackPlugin({
			filename: '[path].br[query]',
			algorithm: 'brotliCompress',
			test: /\.(js|css|html|svg)$/,
			compressionOptions: { level: 11 },
			threshold: 10240,
			minRatio: 0.8,
			deleteOriginalAssets: false
		}),
		new CompressionWebpackPlugin({
			filename: '[path].gz[query]',
			algorithm: 'gzip',
			test: /\.(js|css|html|svg)$/,
			compressionOptions: { level: 9 },
			threshold: 10240,
			minRatio: 0.8,
			deleteOriginalAssets: false
		}),*/
		],
    },
}