$(function() {
    $('.navbar-toggle').on('click', function() {
        $('.main-content-nav').animate({ 
            left: '0px'
        }, 600); 
        /*$('.page-wrapper').animate({ 
            left: '250px'
             
        }, 600);*/
        
      $('.bg-mobile-nav').show();
    });
      
      $('.bg-mobile-nav').on('click', function() {
        $(this).hide();
        $('.main-content-nav').animate({ 
              left: '-250px' 
          }, 300); 
        $('.page-wrapper').animate({ 
              left: '0px' 
          }, 300);
      });
    
    
   
    $('.main-content-nav').on('swipe',function(){
        $('.bg-mobile-nav').hide();
        $('.main-content-nav').animate({ 
              left: '-250px' 
          }, 300); 
        /*$('.page-wrapper').animate({ 
              left: '0px' 
          }, 300);*/
        console.log('33333sasda');
    }); 
    $('.main-content-menu .answer').on('swipe',function(){
      $(this).animate({ left: '600px' }, 400);
      $('.reject span').text('Закончить');
      $('.reject').addClass('active');
    }); 
   
    $('.main-content-menu .reject').on('swipe',function(){
        if( !$(this).hasClass('active')) {
            $('.main-content-menu .answer').addClass('opacity');
          $(this).addClass('opacity');
          console.log('2');
        } else {
        $('.main-content-menu .answer').animate({ left: '0px' }, 400);
            
            $(this).find('span').text('Отклонить');
            $(this).removeClass('active');
            console.log('1');
          
      }   
    });    
    $('.switch').on('click', function(event) {
        event.preventDefault();
        if(event.target.className == "slider") {
          return;
        }
        else if( !$(this).hasClass('turn-off')) {
            $(this).addClass('turn-off').closest('.wait-call').find('span').text('Звонки не принимать');
        } else {
            $(this).removeClass('turn-off').closest('.wait-call').find('span').text('Ожидание звонка');
        }
        
    });
    if($( '.datepicker' ).length) {
        var $input = $( '.datepicker' ).pickadate({
            formatSubmit: 'd/mm/yyyy',
            container: '#container-picker',
            format: 'd/mm/yyyy',
            firstDay: 1,
            monthsFull: [ 'Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь', 'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь' ],
            weekdaysShort: [ 'вс', 'пн', 'вт', 'ср', 'чт', 'пт', 'сб' ],

        });
    }
    
    $('.features').on('click', '.item', function(event) {
        $('.features .item').removeClass('active');
        $(this).addClass('active');
        $('.time-period').css('display', 'flex');
        if( $(this).hasClass('calls-filter')) {
            $('.list-recipient').find('li').show();
            $('.list-recipient').find('li').filter('.key').hide();
        } else if ( $(this).hasClass('enter-filter')) {
            $('.list-recipient').find('li').show();
            $('.list-recipient').find('li').filter('.usual-call').hide();
        }    
    });

});
function formatDate() {
     var dataCall = new Date();
     var input_01 = document.getElementById('input_01').value = dataCall.getDate() + "/" + dataCall.getMonth()+1 + "/" + dataCall.getFullYear();
     var input_02 = document.getElementById('input_02').value = dataCall.getDate() + "/" + dataCall.getMonth()+1 + "/" + dataCall.getFullYear();

}
$(document).on('pageinit' , '#pageone' ,function(){
    $('.bg-mobile-nav').on('swipeleft', function() {
      $(this).hide();
      $('.main-content-nav').animate({ 
            left: '-250px' 
        }, 300); 
      /*$('.page-wrapper').animate({ 
            left: '0px' 
        }, 300);*/
    });
});
