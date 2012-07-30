/*
 * Copyright 2011 Thomas Amsler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package org.artifactly.client.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartServiceAtBoot extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		
		if("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
			
		     Intent serviceLauncher = new Intent(context, ArtifactlyService.class);
		     context.startService(serviceLauncher);
		     Log.i("** S.S.A.B **", "Start service at boot");
		  }
	}
}
