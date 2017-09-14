const PUBLISH_KEY 			= 'pub-c-93f04d9d-dac3-46f6-930d-164cce692f44';
const SUBSCRIBE_KEY			= 'sub-c-e2ff393a-a729-11e6-a114-0619f8945a4f';
const SECRET_KEY 			= 'sec-c-N2M2OWY0NWQtOTA4NS00ZTg0LTkwNTEtZWQ2ODQ4NDIwMDdh';
const SKIPO_GROUP	 		= 'skipodev';
const STDBY_SUFFIX	        = '-stdby';

const SHARED_PREFS			= "NativeStorage";
const CALL_CENTER_NUMBER	= "CALL_CENTER_NUMBER";
const CLIENT_PHONE_NUMBER	= "CLIENT_PHONE_NUMBER";
const DOMOFON_PHONE_NUMBER	= "DOMOFON_PHONE_NUMBER";
const PUBNUB_PUBLISH_KEY 	= "PUBNUB_PUBLISH_KEY";
const PUBNUB_SUBSCRIBE_KEY 	= "PUBNUB_SUBSCRIBE_KEY";
const PUBNUB_SECRET_KEY 	= "PUBNUB_SECRET_KEY";
const CALL_ENABLED			= "CALL_ENABLED";
const CALL_STATE			= "CALL_STATE";

var	client = {
	name			: null
,	subChannel		: null
,	pubChannel		: null
,	history			: []
,	phone			: null
,	state			: null
,	number			: null
};


angular.module('starter', [
	'ionic',
	'ngCordova',
	'starter.services',
	'starter.controllers',
	'pubnub.angular.service',
	'ionic-datepicker'
])

.run(function($ionicPlatform, $rootScope, mSharedPreferences) {

 	$ionicPlatform.ready(function() {
		console.log('platform Ready!');
	    if(window.cordova && window.cordova.plugins.Keyboard) {
	    	cordova.plugins.Keyboard.hideKeyboardAccessoryBar(true);
	    	cordova.plugins.Keyboard.disableScroll(true);
	    	window.screenLocker.unlock(function() {
	    	    console.log('screen unlock success');
	    	}, function(e) {
	    	    console.log('screen unlock error: ' + e);
	    	}, 0);
	    }

	    if(window.StatusBar) {
	        StatusBar.styleDefault();
	    }

	});


})

.config(function ($stateProvider, $urlRouterProvider) {

	$stateProvider
	.state('app', {
		url: '/app',
		abstract: true,
		templateUrl: 'templates/menu.html',
		controller: 'AppCtrl'
	})
	.state('app.history', {
    	url: '/history',
		cache: true,
	    views: {
	    	'menuContent': {
	        	templateUrl: 'templates/history.html',
	        	controller: 'HistoryCtrl'
	      	}
	    }
	})
	.state('app.about', {
    	url: '/about',
    	views: {
      		'menuContent': {
        		templateUrl: 'templates/about.html',
      		}
    	}
  	});

  	$urlRouterProvider.otherwise('/app/history');

});
