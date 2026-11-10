import { createApp } from "vue";
import App from "./App.vue";
import router from "./app/router/index.js";
import "./styles/main.css";
createApp(App).use(router).mount("#app");
