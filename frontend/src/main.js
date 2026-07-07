import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Vant from 'vant'
import ElementPlus from 'element-plus'
import 'vant/lib/index.css'
import 'element-plus/dist/index.css'
import './assets/styles.css'
import App from './App.vue'
import router from './router'

createApp(App)
  .use(createPinia())
  .use(router)
  .use(Vant)
  .use(ElementPlus)
  .mount('#app')
