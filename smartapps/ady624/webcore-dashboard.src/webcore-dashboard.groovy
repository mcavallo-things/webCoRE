/*
 *  webCoRE - Community's own Rule Engine - Web Edition
 *
 *  Copyright 2016 Adrian Caramaliu <ady624("at" sign goes here)gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
public static String version() { return "v0.3.110.20191009" }
public static String HEversion() { return "v0.3.110.20200702_HE" }

/******************************************************************************/
/*** webCoRE DEFINITION														***/
/******************************************************************************/
private static String handle() { return "webCoRE" }
//if(!isHubitat())include 'asynchttp_v1'
definition(
	name: "${handle()} Dashboard",
	namespace: "ady624",
	author: "Adrian Caramaliu",
	description: "Do not install this directly, use webCoRE instead",
	parent: "ady624:${handle()}",
	category: "Convenience",
    /* icons courtesy of @chauger - thank you */
	iconUrl: "https://cdn.rawgit.com/ady624/${handle()}/master/resources/icons/app-CoRE.png",
	iconX2Url: "https://cdn.rawgit.com/ady624/${handle()}/master/resources/icons/app-CoRE@2x.png",
	iconX3Url: "https://cdn.rawgit.com/ady624/${handle()}/master/resources/icons/app-CoRE@3x.png"
)

preferences {
	//UI pages
	page(name: "pageMain")
}


/******************************************************************************/
/*** 																		***/
/*** CONFIGURATION PAGES													***/
/*** 																		***/
/******************************************************************************/
def pageMain() {
    //clear devices cache
	dynamicPage(name: "pageMain", title: "", install: false, uninstall: false) {
        if (!parent || !parent.isInstalled()) {
            section() {
                paragraph "Sorry, you cannot install a piston directly from the Marketplace, please use the webCoRE SmartApp instead."
            }
            section("Installing webCoRE") {
                paragraph "If you are trying to install webCoRE, please go back one step and choose webCoRE, not webCoRE Piston. You can also visit wiki.webcore.co for more information on how to install and use webCoRE"
                if (parent) href "", title: "More information", description: parent.getWikiUrl(), style: "external", url: parent.getWikiUrl(), image: "https://cdn.rawgit.com/ady624/webCoRE/master/resources/icons/app-CoRE.png", required: false
            }
        } else {
            section("Status") {
                paragraph state.status
            }
        }
	}
}



/******************************************************************************/
/*** 																		***/
/*** INITIALIZATION ROUTINES												***/
/*** 																		***/
/******************************************************************************/


private installed() {
	initialize()
	return true
}

private updated() {
	unsubscribe()
	initialize()
	return true
}

private initialize() {
}


/******************************************************************************/
/*** 																		***/
/*** PRIVATE METHODS														***/
/*** 																		***/
/******************************************************************************/

private subscribeToAttribute(devices, attribute) {
    //subscribe(devices.findAll{ it.hasAttribute(attribute) }, attribute, dashboardEventHandler)
	def t0 = devices.findAll{ it.hasAttribute(attribute) }
	if(t0) {
		t0.each{ subscribe(it, attribute, dashboardEventHandler) }
	}
}

/******************************************************************************/
/*** 																		***/
/*** PUBLIC METHODS															***/
/*** 																		***/
/******************************************************************************/

public void start(devices, instanceId) {
	runIn(30, stop)
	if ((state.status?:'Idle') != 'Idle') return
    if (instanceId) state.instanceId = instanceId
    if (!state.instanceId) return
    state.region = apiServerUrl('/').contains('graph-eu') ? 'eu' : 'us';
    state.status = 'Subscribing'
    unsubscribe()
    subscribeToAttribute(devices, 'switch')
    subscribeToAttribute(devices, 'level')
    subscribeToAttribute(devices, 'temperature')
    subscribeToAttribute(devices, 'color')
    subscribeToAttribute(devices, 'colorTemperature')
    subscribeToAttribute(devices, 'hue')
    subscribeToAttribute(devices, 'saturation')
    subscribeToAttribute(devices, 'contact')
    subscribeToAttribute(devices, 'presence')
    subscribeToAttribute(devices, 'lock')
    subscribeToAttribute(devices, 'motion')
    subscribeToAttribute(devices, 'water')
    state.status = 'Listening'
}

public void stop() {
	if (state.status == 'Idle') return
	unsubscribe()
    unschedule(stop)
	state.status = 'Idle'
}

public dashboardEventHandler(evt) {
	broadcastEvent(hashId(evt.device.id), evt.name, evt.value, evt.date.time)
}

public updatePiston(pistonId, piston) {
	broadcastEvent(pistonId, 'piston', piston.s.new, piston.t)
}

private void broadcastEvent(deviceId, eventName, eventValue, eventTime) {
	def iid = state.instanceId
	def region = state.region ?: 'us'
	if (!iid || !iid.startsWith(':') || !iid.endsWith(':')) return
    
	def params = [
		uri: "https://api-${region}-${iid[32]}.webcore.co:9237",
		path: '/event/sink',
		requestContentType: "application/json",
		headers: ['ST' : state.instanceId],
		body: [d: deviceId, n: eventName, v: eventValue, t: eventTime]
	]
    
/*
    if(asynchttp_v1){
	asynchttp_v1.put(null, params)
    }
    else { */
	asynchttpPut('myDone', params, [bbb:0])
    //}
}

def myDone(resp, data) {
	//log.debug "resp: ${resp?.status}"
}

/******************************************************************************/
/***																		***/
/*** SECURITY METHODS														***/
/***																		***/
/******************************************************************************/
def String md5(String md5) {
   try {
	java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5")
	byte[] array = md.digest(md5.getBytes())
	def result = ""
	for (Integer i = 0; i < array.length; ++i) {
		result += Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3)
	}
	return result
    } catch (java.security.NoSuchAlgorithmException e) {
    }
    return null;
}

def String hashId(id, updateCache=true) {
	//enabled hash caching for faster processing
	String result
	String myId = id.toString()
	def hash = [:]
	if(state.hash) {
		hash = state.hash
		result = (String) hash."${myId}"
	}
	if (!result) {
		result = ":${md5("core." + id)}:"
		if(updateCache) {
			hash."${myId}" = result
			state.hash = hash
		}
	}
	return result
}

private isHubitat(){
 	return hubUID != null   
}

/******************************************************************************/
/***																		***/
/*** END OF CODE															***/
/***																		***/
/******************************************************************************/
