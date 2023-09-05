//webpack.config.cjs
const path = require('path');

module.exports = {
  mode: "development",
  devtool: "inline-source-map",
  entry: "./src/index.ts",
  output: {
    path: path.resolve(__dirname, './build'),
    filename: "timeline.ts", // <--- Will be compiled to this single file
    globalObject: 'this',
    library: {
      name: 'timeline',
      type: 'umd',
    },
  },
  resolve: {
    extensions: [".ts", ".tsx", ".js"]
  },
  module: {
    rules: [
      {
        test: /\.tsx?$/,
        loader: "ts-loader"
      }
    ]
  }
};
