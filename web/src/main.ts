import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from '@tusalud/app/app.config';
import { App } from '@tusalud/app/app';

bootstrapApplication(App, appConfig).catch((err) => console.error(err));
