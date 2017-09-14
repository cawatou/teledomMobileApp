var track = null;

 angular.module('starter.services', [])


.factory('Login', function($rootScope, $ionicModal) {
	$ionicModal.fromTemplateUrl('templates/history.html', {
        scope: $rootScope,
        animation: 'slide-in-up'
    }).then(function(modal) {
        $rootScope.modal = modal;
    });

	return {
 		show: function(){
			$rootScope.modal.show();
		},
		hide: function(){
			$rootScope.modal.hide();
		}
	};

})

.factory('promiseFactory', function($q) {
  return {
    decorate: function(promise) {
      promise.success = function(callback) {
        promise.then(callback);
        return promise;
      };

      promise.error = function(callback) {
        promise.then(null, callback);
        return promise;
      };
    },
    defer: function() {
      var deferred = $q.defer();
      this.decorate(deferred.promise);
      return deferred;
    }
  };
})

.factory('skipoQuery', function($rootScope, $ionicPlatform, $timeout, $ionicLoading, Pubnub, mSharedPreferences) {
    var timeout = null
,       receive_cnt = 0
,       transsmit_cnt = 0
,       listener = null;

    var loadingStop = function(){
            $timeout.cancel(timeout);
            $ionicLoading.hide();
        };

    var loadingStart = function(delay){
            $ionicLoading.show({
                template: '<ion-spinner icon="bubbles"></ion-spinner>',
                hideOnStateChange: true
            });
            timeout = $timeout(function(){
                console.error('skipoQuery:publish:error: Server timeout');
                loadingStop();
                $ionicPopup.alert({
                    title: 'Ошибка связи',
                    template: 'Возможно сервер занят. Повторите запрос позже'
                });
            }, delay);
        };


	return {
        init: function() {

            if ( listener != null ) return;

        	Pubnub.init({
        		    publishKey		: PUBLISH_KEY
        		,	subscribeKey	: SUBSCRIBE_KEY
        		,   secretKey		: SECRET_KEY
        		,	ssl				: true
        	});

            client.name = Pubnub.getUUID();
            console.log("Client Name(UUID): " + client.name);

            client.subChannel = client.name;// + STDBY_SUFFIX;
            console.log("Subscribe channell: " + client.subChannel);

            if ( typeof localStorage['pub-channel'] !== 'undefined' && localStorage['pub-channel'] !== null ) {
                client.pubChannel = localStorage['pub-channel'];
            } else {
                client.pubChannel = SKIPO_GROUP;
            }
            console.log("Publish channel: " + client.pubChannel);

            listener = {
                status: function(statusEvent) {
            		console.log(JSON.stringify(statusEvent));
            	},
            	message: function(envelope) {
            		var status,
                        msg = envelope.message;
                    //console.log('pubnub message: \n' + JSON.stringify(envelope));
                    loadingStop();
                    if ( msg.response == 'done' ) {
                        console.log('skipoQuery:message:success: [' + (++receive_cnt) + ']\n' + JSON.stringify(msg));
                        status = 'success';
                    } else {
                        console.log('skipoQuery:message:error: \n' + JSON.stringify(msg));
                        status = 'error';
                    }
                    $rootScope.$emit(msg.event, {status: status, data: msg});
            	}
            };

            Pubnub.addListener(listener);

            Pubnub.subscribe({channels: [client.subChannel]});

        },
		publish: function(query) {
			Pubnub.publish({
				channel: client.pubChannel,
				message: query
			},
            function (status, response) {
                if (status.error) {
                    console.error('skipoQuery:publish:error:\n' +  JSON.stringify(response));
                } else {
                    console.log('skipoQuery:publish:success: [' + (++transsmit_cnt) + ']\n' + 'send to: '+ client.pubChannel + '\n' + JSON.stringify(query));
                }
            });
        },
        stop: function() {
            loadingStart(5000);
            Pubnub.unsubscribeAll();
        }
	};

})


.factory('mToast', function($cordovaToast) {
    return {
        show: function(message, callback) {
            $cordovaToast
                .show(message, 'long', 'bottom')
                .then(function(success) {
                    console.log('Toast Susccess: ' + success);
                    if ( callback ) {
                        callback();
                    }
                }, function (error) {
                    console.log('Toast Error: ' + error);
                    if ( callback ) {
                        callback();
                    }
                });
        }
    };
})

.factory('mSharedPreferences', function($cordovaPreferences) {
    const TAG = 'SharedPreferences ';
    var rezult = '';
    return {
        remove: function(name) {
            $cordovaPreferences.remove(name, SHARED_PREFS)
                .success(function(value) {
                    console.log(TAG + 'remove Item Success: ' + value);
                })
                .error(function(error) {
                    console.log(TAG + 'remove Item Error: ' + error);
                });
        },
        fetch: function(name, defValue, callback) {
            $cordovaPreferences.fetch(name, SHARED_PREFS)
                .success(function(value) {
                    console.log(TAG + 'fetch Success: ' + value);
                    if ( value == 'null' ) {
                        callback(defValue);
                    }
                    callback(value);
                })
                .error(function(error) {
                    console.log(TAG + 'fetch Error: ' + error);
                    callback(defValue);
                });
        },
        store: function(name, value) {
            $cordovaPreferences.store(name, value, SHARED_PREFS)
                .success(function(res) {
                    console.log(TAG + 'store Success: ' + res);
                })
                .error(function(error) {
                    console.log(TAG + 'store Error: ' + error);
                });
        }
    };
})

.factory('datePickerFormat', function() {
	var dataCall = null;

	return {
		toPicker: function(val){
			dataCall = val ? new Date(val) : new Date();
			return (dataCall.getDate() + "/" + (dataCall.getMonth() + 1) + "/" + dataCall.getFullYear());
		},
		toDate: function(val){
			dataCall = val.split('/');
			return (new Date(dataCall[2], dataCall[1] - 1, dataCall[0]));
		},
		toQuery: function(val){
			dataCall = val.split('/');
			return (
				dataCall[2] + '-' + dataCall[1]	+ '-'	+ dataCall[0] + ' ' + '00:00:00'
			);
		}
	};

})

.factory('ringTone', function() {

	return {
		init: function(src) {
			var sound = src;
			if ((typeof device !== "undefined") && device.platform == 'Android') {
				sound = '/android_asset/www' + src + '.mp3';
			}
			track = new Audio(sound);
		},
		play: function(isLoop) {
			if ( !track ) return;
			if ( isLoop ) {
				track.loop = true;
			}
			track.play();
		},
		stop: function() {
			if ( !track ) return;
			track.loop = false;
			track.pause();
			try       { track.currentTime = 0.0 }
			catch (e) { }
		}
	};

})


.directive('checkImage', function ($q) {

	return {
  	restrict: 'A',
   	link: function (scope, element, attrs) {
    	attrs.$observe('ngSrc', function (ngSrc) {
      	var deferred = $q.defer();
       	var image = new Image();
       	image.onerror = function () {
			 		element.attr('src', 'img/without-photo.png'); // set default image
					deferred.resolve(false);
       	};
       	image.onload = function () {
        	deferred.resolve(true);
       	};
       	image.src = ngSrc;
       	return deferred.promise;
     	});
   	}
 	};

})


;
