import { bootstrapApplication } from '@angular/platform-browser';

import { AppRoot } from '@tusalud/ui/app-root';
import { appConfig } from './app/app.config';

bootstrapApplication(AppRoot, appConfig).catch((error) => console.error(error));
