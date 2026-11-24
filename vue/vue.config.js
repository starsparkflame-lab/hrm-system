const CompressionPlugin = require('compression-webpack-plugin')

// 兼容 Node.js 24
process.env.NODE_OPTIONS = (process.env.NODE_OPTIONS || '') + ' --openssl-legacy-provider'

module.exports = {
  lintOnSave: false, // 关闭语法检查
  productionSourceMap: false, // 关闭生产环境 sourceMap，减小文件体积
  devServer: {
    open: true,
    disableHostCheck: true,
    proxy: {
      '/dev': {
        // 请求前缀
        target: 'http://localhost:' + process.env.VUE_APP_PORT, pathRewrite: { '^/dev': '' }, changeOrigin: true // 用于控制请求头中的host值
      }
    }
  },
  chainWebpack: config => {
    // 禁用 thread-loader（兼容 Node.js 24）
    config.module.rule('js').uses.delete('thread-loader')
    config.module.rule('vue').uses.delete('thread-loader')

    // 生产环境优化
    config.when(process.env.NODE_ENV === 'production', config => {
      // 关闭 sourceMap
      config.devtool(false)
      // 分包优化：将大库单独拆分
      config.optimization.splitChunks({
        chunks: 'all',
        cacheGroups: {
          libs: {
            name: 'chunk-libs',
            test: /[\\/]node_modules[\\/]/,
            priority: 10,
            chunks: 'initial'
          },
          elementUI: {
            name: 'chunk-elementUI',
            priority: 20,
            test: /[\\/]node_modules[\\/]_?element-ui(.*)/
          },
          echarts: {
            name: 'chunk-echarts',
            priority: 20,
            test: /[\\/]node_modules[\\/]_?echarts(.*)/
          }
        }
      })
      // 压缩图片
      config.module
        .rule('images')
        .use('url-loader')
        .loader('url-loader')
        .tap(options => Object.assign(options, { limit: 4096 }))
      // gzip 压缩
      config.plugin('compression').use(CompressionPlugin, [{
        algorithm: 'gzip',
        test: /\.(js|css|json|txt|html|ico|svg)(\?.*)?$/i,
        threshold: 10240,
        minRatio: 0.8,
        deleteOriginalAssets: false
      }])
    })
  }
}
