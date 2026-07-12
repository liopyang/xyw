/// <reference types="vite/client" />
import 'vue'

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_FILE_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare module '*.vue' {
  import { DefineComponent } from 'vue'
  // eslint-disable-next-line @typescript-eslint/no-explicit-any, @typescript-eslint/ban-types
  const component: DefineComponent<{}, {}, any>
  export default component
}

declare module '@vue/runtime-core' {
  interface ComponentCustomProperties {
    uni: typeof uni
  }
}
