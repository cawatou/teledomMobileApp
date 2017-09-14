angular.module('starter.controllers', [])


.controller('SigninCtrl', function($scope, $rootScope, $interval, $timeout, $ionicPopup, mSharedPreferences, $ionicLoading, skipoQuery) {
	$scope.signin = {};

	$scope.initSignin = function() {
		$scope.signin.input = '';
		$scope.signin.text = "Введите 10 цифр номера вашего телефона:";
		$scope.signin.repeat = false;
	};

	$scope.initSignin();

	$scope.completeStop = function() {
		$interval.cancel($scope.completeTimer);
	};

	$scope.completeStart = function() {
		var sec = 30;
		var text = 'До отправки смс кода осталось ' + sec + ' сек';
		$scope.signin.timer = text;
		$scope.completeTimer = $interval( function() {
			if ( sec-- < 0 )  {
				$scope.completeStop();
				$ionicPopup.alert({
					title: 'Вход',
					template: 'Время истекло. Повторите процедуру входа'
				}).then(function() {
					$scope.initSignin();
				});
				return;
			}
			text = 'До отправки смс кода осталось ' + sec + ' сек';
			$scope.signin.timer = text;
		}, 1000);
	};

	$scope.sendPhone = function(input) {
		if ( input.toString().length < 10 ) {
			$ionicPopup.alert({
				title: 'Ошибка ввода',
				template: 'Номер телефона должен состоять из 10 цифр'
			});
			return;
		}
		client.number = "7" + input;
		client.state = "mobile_getcode";
		skipoQuery.publish({ event : client.state, phone : client.number });
	};
	$rootScope.$on("mobile_getcode", function(event, src) {
		var status = src.status,
			msg = src.data;
		if ( status == 'success' ) {
			$ionicPopup.alert({
				title: 'Вход',
				template: 'Вам был выслан sms код. Введите его в следующую форму'
			}).then(function() {
				$scope.completeStart();
			});
			$scope.signin.input = '';
			$scope.signin.text = "Введите полученный sms код:";
			$scope.signin.repeat = true;
			document.getElementById('input')
		} else {
			$ionicPopup.alert({
				title: 'Ошибка авторизации',
				template: 'Проверьте правильность введенного телефона или зарегистрируйтесь в системе TELEDOM'
			});
		}
	});

	$scope.sendSMS = function(input) {
		$scope.completeStop();
		client.state = "mobile_checkcode";
		skipoQuery.publish({ event :  client.state, phone : client.number, code : input.toString() });
	};
	$rootScope.$on("mobile_checkcode", function(event, src) {
		var status = src.status,
			msg = src.data;
		if ( status == 'success' ) {
			client.pubChannel = msg.token;
			localStorage['pub-channel'] = msg.token;
            mSharedPreferences.store(PUBNUB_PUBLISH_KEY, PUBLISH_KEY);
            mSharedPreferences.store(PUBNUB_SUBSCRIBE_KEY, SUBSCRIBE_KEY);
            mSharedPreferences.store(PUBNUB_SECRET_KEY, SECRET_KEY);
            localStorage[PUBNUB_PUBLISH_KEY] = PUBLISH_KEY;
            localStorage[PUBNUB_SUBSCRIBE_KEY] = SUBSCRIBE_KEY;
            localStorage[PUBNUB_SECRET_KEY] = SECRET_KEY;
			$ionicLoading.show({
	            template: '<ion-spinner icon="bubbles"></ion-spinner>',
	            hideOnStateChange: true
	        });
			$timeout(function () {
				$scope.sendAuth();
			}, 2000);
		} else {
			$ionicPopup.alert({
				title: 'Ошибка авторизации',
				template: 'Введен неправильный смс код. Повторите запрос.'
			}).then(function() {
				$scope.initSignin();
			});
		}
	});

	$scope.sendAuth = function() {
		client.state = "mobile_auth";
		skipoQuery.publish({ event: client.state });
	};
	$rootScope.$on("mobile_auth", function(event, src) {
		var status = src.status,
			msg = src.data;
		if ( status == 'success' ) {
			var fields = msg.fields;
			mSharedPreferences.store(CALL_CENTER_NUMBER, fields.server_phone);
			mSharedPreferences.store(DOMOFON_PHONE_NUMBER, fields.serial_number);
			localStorage[DOMOFON_PHONE_NUMBER] = fields.serial_number + STDBY_SUFFIX;
			localStorage['flat-id'] = fields.id_flat;
			localStorage['first-name'] = fields.first_name;
			localStorage['middle-name'] = fields.middle_name;
			localStorage['second-name'] = fields.second_name;
			localStorage['avatar'] = fields.avatar;
			$scope.signinModal.hide();
			$scope.$apply(function(){
				$scope.userUpdate();
			});
		} else {
			$ionicPopup.alert({
				title: 'Ошибка авторизации',
				template: 'Что-то пошло не так. Попробуйте еще раз.'
			}).then(function() {
				$scope.initSignin();
			});
		}
	});


})

.controller('AppCtrl', function($scope, $ionicPlatform, $timeout, $ionicModal, $ionicSideMenuDelegate, skipoQuery, mSharedPreferences) {
    $ionicPlatform.ready(function(){

        $scope.userUpdate =  function() {
            $scope.user	 = {
                name: localStorage['second-name'] + ' ' +
                        localStorage['first-name'] + ' ' +
                        localStorage['middle-name'],
                img: 	localStorage['avatar']
            };
        };

        $scope.userUpdate();

        skipoQuery.init();

        if ( typeof localStorage['pub-channel'] === 'undefined' || localStorage['pub-channel'] === null ) {
            $ionicModal.fromTemplateUrl('templates/signin.html', {
                scope: $scope,
                animation: 'slide-in-up',
                hardwareBackButtonClose: false
            }).then(function(modal) {
                $scope.signinModal = modal;
                $scope.signinModal.show();
            });
        }

        $scope.selectedMenu = 'history';

        $scope.toggleLeftSideMenu = function(tab) {
            console.log(tab);
            if ( tab == 'exit' ) {
                localStorage.clear();
                skipoQuery.stop();
                $timeout(function() {
                    location.reload(true);
                }, 3000);
            } else {
            $ionicSideMenuDelegate.toggleLeft();
                $scope.selectedMenu = tab;
            }
        };

        var callPermitStates = {
            false: 'Звонки не принимать',
            true: 'Ожидание звонка'
        };

        $scope.call = {
            permit: {
                enabled: localStorage['callPermit'] == 'false' ? false : true,
                value: callPermitStates[localStorage['callPermit']],
                onChange: function () {
                    this.value = callPermitStates[this.enabled];
                    localStorage['callPermit'] = this.enabled;
                    mSharedPreferences.store(CALL_ENABLED, localStorage['callPermit'] == 'false' ? false : true);
                }
            }
        };

        $scope.call.permit.onChange();
/*
        client.phone = PHONE({
            number        : client.name + STDBY_SUFFIX
        ,   publish_key   : PUBLISH_KEY
        ,   subscribe_key : SUBSCRIBE_KEY
        ,   secret_key 	  : SECRET_KEY
        ,ssl           : true
        });

        client.phone.connect(function() {
            console.log('network LIVE.');
        });

        client.phone.disconnect(function(){
            console.log('network GONE.');
        });

        client.phone.reconnect(function(){
            console.log('network BACK!');
        });

        client.phone.ready(function() {
            client.phone.debug(function(info) {
                console.info(info);
            });
        });

        mSharedPreferences.fetch(CALL_STATE, false, function(value) {
            console.log("call state: " + value + typeof value);
            mSharedPreferences.remove(CALL_STATE);
            if ( value !== 'null' && value === true ){
                client.phone.dial(localStorage[DOMOFON_PHONE_NUMBER]);
            };
        });

        client.phone.receive(function(session){
            console.log('Call Comes In');
            session.connected(function(session){
                console.log('Session: CONNECTED');
                session.send({text: 'ready'});
                client.phone.$('remoteVideo').innerHTML = '';
                client.phone.$('remoteVideo').appendChild(session.video);
                $ionicModal.fromTemplateUrl('templates/call.html', {
                    scope: $scope,
                    animation: 'slide-in-up',
                    hardwareBackButtonClose: false
                }).then(function(modal) {
                    $scope.phoneModal = modal;
                    $scope.phoneModal.show();
                });
            });

            $scope.openDoor = function() {
                session.send({text: 'open'});
            };

            $scope.hangup = function() {
                session.hangup();
            };

            session.ended(function(session){
                console.log('Session: ENDED');
                $scope.phoneModal.hide();
            });

        });*/
    });


})


.controller('HistoryCtrl',	function($scope, $rootScope, $ionicPlatform, $ionicPopup,  ionicDatePicker, datePickerFormat, skipoQuery) {

	$scope.user = {
		username: 'test@gmail.com',
    	password: 'test@gmail.com'
  	};

	$scope.filter = {
		calls: true,
		keys: true,
		date: {
			start: datePickerFormat.toPicker(new Date().getTime() - (86400000 * 200)),
			stop: datePickerFormat.toPicker(new Date().getTime())
		}
	};

	$scope.doFilter = function() {
		var count = 1, item;
		$scope.items = [];
		$scope.emptystring = false;
		if ( client.history.length == 0 ) {
			$scope.emptystring = true;
		} else {
			client.history.forEach(function(entry){
				if (	($scope.filter.calls === true && entry.type == 'call') ||
							($scope.filter.keys === true && entry.type == 'key')		) {
					item = {
						id: 		count++,
						title: 	entry.type == 'call' ? 'Звонок с домофона' : 'Вход по ключу',
						img: 		entry.avatar,
						time: 	new Date(entry.time).toLocaleString()
					};
					$scope.items.push(item);
				}
			});
		}
	};

	var getHistory = function() {
		client.state = 'mobile_history';
		var query = {
			event: client.state,
			fields: {
				email: $scope.user.username,
				pass: $scope.user.password,
				from: datePickerFormat.toQuery($scope.filter.date.start),//toISOString(),
				to: datePickerFormat.toQuery($scope.filter.date.stop)//toISOString()
			}
		};
		skipoQuery.publish(query);
	};
	$rootScope.$on("mobile_history", function(event, src) {
		var status = src.status,
			msg = src.data;
		if ( status == 'success' ) {
			var fields = msg.fields;
			client.history	= fields.photo;
			$scope.doFilter();
		} else {

		}
	});


  $scope.setActive = function(type) {
    if ( $scope.active !== type ) {
			$scope.active = type;
		} else {
			$scope.active = null;
		}

		ionicDatePicker.openDatePicker({
			inputDate: datePickerFormat.toDate(type == 'start' ? $scope.filter.date.start : $scope.filter.date.stop),
			mondayFirst: true,
	    weeksList: [ 'вс', 'пн', 'вт', 'ср', 'чт', 'пт', 'сб' ],
	    monthsList: [ 'Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь', 'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь' ],
	    templateType: 'modal',
	    from: new Date(2012, 8, 1),
	    to: new Date(),
	    showTodayButton: false,
	    dateFormat: 'dd/mm/yyyy',
	    closeOnSelect: true,
	    disableWeekdays: [],
			callback: function (val) {  //Mandatory
				var newDateToPicker 	= datePickerFormat.toPicker(val),
						startPickerToDate = datePickerFormat.toDate($scope.filter.date.start).getTime(),
						stopPickerToDate  =	datePickerFormat.toDate($scope.filter.date.stop).getTime(),
						newDate			  		= new Date(val).getTime();
				if ( type == 'start' ) {
					$scope.filter.date.start = newDate > stopPickerToDate ? $scope.filter.date.stop : newDateToPicker;
				} else {
					$scope.filter.date.stop = newDate < startPickerToDate ? $scope.filter.date.start : newDateToPicker;
				}
				$scope.active = null;
				getHistory();
		  }
		});
  };

	$scope.isActive = function(type) {
    return type === $scope.active;
  };

	$rootScope.$on('datepicker', function(event, data){
		$scope.active = null;
	});

	$ionicPlatform.ready(function() {
	    getHistory();
	});


})


;
