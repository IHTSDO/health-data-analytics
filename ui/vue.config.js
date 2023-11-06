const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  runtimeCompiler: true,
  devServer: {
    port: 4200,
    allowedHosts: 'localhost.ihtsdotools.org'
  }
})
