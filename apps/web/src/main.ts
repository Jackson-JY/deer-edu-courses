import { createApp } from 'vue'
import { createPinia } from 'pinia'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import { router } from './router'
import 'element-plus/dist/index.css'
import './assets/base.css'

const app = createApp(App)

// 全局注册 Element Plus 图标（按需自动导入只覆盖组件，图标单独注册）
for (const [name, comp] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, comp)
}

app.use(createPinia())
app.use(router)
app.mount('#app')
