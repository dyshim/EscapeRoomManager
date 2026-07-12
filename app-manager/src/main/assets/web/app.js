(function(){
  'use strict';

  var pin = localStorage.getItem('managerPin') || '';
  var previousStates = {};
  var activeEndedRooms = new Set();
  var alarmTimeout = null;
  var audioContext = null;
  var alarmInterval = null;
  var webAlertsEnabled = localStorage.getItem('webAlertsEnabled') === 'true';
  var autoStopSeconds = Number(localStorage.getItem('webAlarmAutoStopSeconds') || '30');
  var refreshInFlight = false;

  var loginOverlay;
  var loginForm;
  var pinInput;
  var loginButton;
  var loginError;
  var connection;
  var roomsElement;
  var alarmBanner;
  var autoStopSelect;

  function byId(id){ return document.getElementById(id); }
  function escapeHtml(value){
    return String(value).replace(/[&<>"']/g,function(character){
      return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[character];
    });
  }
  function escapeJsString(value){ return String(value).replace(/\\/g,'\\\\').replace(/'/g,"\\'"); }
  function timeText(seconds){
    seconds = Math.max(0, Number(seconds) || 0);
    return String(Math.floor(seconds / 60)).padStart(2,'0') + ':' + String(seconds % 60).padStart(2,'0');
  }
  function statusText(room){
    if(room.maintenance) return '유지보수';
    if(Number(room.seconds) <= 0) return '종료';
    if(room.running) return Number(room.seconds) <= 300 ? '5분 이하' : '진행중';
    if(room.status === 'PAUSED') return '일시정지';
    return '대기';
  }
  function showLogin(message){
    pin = '';
    localStorage.removeItem('managerPin');
    loginOverlay.style.display = 'flex';
    loginError.textContent = message || '';
    window.setTimeout(function(){ pinInput.focus(); }, 0);
  }
  function hideLogin(){ loginOverlay.style.display = 'none'; }

  async function submitLogin(event){
    if(event) event.preventDefault();
    var candidate = pinInput.value.trim();
    loginError.textContent = '';
    if(!candidate){ loginError.textContent = 'PIN을 입력해 주세요.'; pinInput.focus(); return; }
    loginButton.disabled = true;
    loginButton.textContent = '확인 중…';
    try{
      var response = await fetch('/api/login', {
        method:'POST',
        headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'},
        body:'pin=' + encodeURIComponent(candidate),
        cache:'no-store'
      });
      if(!response.ok){
        throw new Error(response.status === 401 ? 'PIN이 올바르지 않습니다.' : '로그인 요청에 실패했습니다. (' + response.status + ')');
      }
      var result = await response.json();
      if(!result.ok) throw new Error('로그인 응답이 올바르지 않습니다.');
      pin = candidate;
      localStorage.setItem('managerPin', pin);
      hideLogin();
      enableAudioOnly();
      await refresh();
    }catch(error){
      showLogin(error && error.message ? error.message : '로그인 중 오류가 발생했습니다.');
    }finally{
      loginButton.disabled = false;
      loginButton.textContent = '접속';
    }
  }

  function saveAlarmSettings(){
    autoStopSeconds = Number(autoStopSelect.value) || 0;
    localStorage.setItem('webAlarmAutoStopSeconds', String(autoStopSeconds));
    if(activeEndedRooms.size && autoStopSeconds > 0) scheduleAutoStop();
  }
  function enableAudioOnly(){
    try{
      if(!audioContext) audioContext = new (window.AudioContext || window.webkitAudioContext)();
      if(audioContext.state === 'suspended') audioContext.resume();
    }catch(ignore){}
  }
  async function enableWebAlerts(){
    enableAudioOnly();
    webAlertsEnabled = true;
    localStorage.setItem('webAlertsEnabled','true');
    if('Notification' in window && Notification.permission === 'default'){
      try{ await Notification.requestPermission(); }catch(ignore){}
    }
    playSingleBeep();
  }
  function playSingleBeep(){
    if(!audioContext) return;
    try{
      var oscillator = audioContext.createOscillator();
      var gain = audioContext.createGain();
      oscillator.type = 'square';
      oscillator.frequency.value = 880;
      gain.gain.value = 0.06;
      oscillator.connect(gain);
      gain.connect(audioContext.destination);
      oscillator.start();
      oscillator.stop(audioContext.currentTime + 0.15);
    }catch(ignore){}
  }
  function startAlarmSound(){
    if(!webAlertsEnabled) return;
    enableAudioOnly();
    if(alarmInterval) return;
    playSingleBeep();
    alarmInterval = window.setInterval(function(){ playSingleBeep(); window.setTimeout(playSingleBeep,240); },1000);
  }
  function stopAlarmSound(){ if(alarmInterval){ window.clearInterval(alarmInterval); alarmInterval = null; } }
  function scheduleAutoStop(){
    if(alarmTimeout) window.clearTimeout(alarmTimeout);
    alarmTimeout = null;
    if(autoStopSeconds > 0) alarmTimeout = window.setTimeout(stopAllWebAlarms, autoStopSeconds * 1000);
  }
  function stopAllWebAlarms(){
    stopAlarmSound();
    if(alarmTimeout){ window.clearTimeout(alarmTimeout); alarmTimeout = null; }
    activeEndedRooms.clear();
    updateAlarmBanner();
    document.title = '방탈출 운영';
  }
  function acknowledgeRoom(id){
    activeEndedRooms.delete(id);
    if(activeEndedRooms.size === 0){
      stopAlarmSound();
      if(alarmTimeout) window.clearTimeout(alarmTimeout);
      alarmTimeout = null;
      document.title = '방탈출 운영';
    }
    updateAlarmBanner();
  }
  function updateAlarmBanner(){
    if(!activeEndedRooms.size){ alarmBanner.style.display='none'; alarmBanner.innerHTML=''; return; }
    var names = Array.from(activeEndedRooms).map(function(id){ return previousStates[id] ? previousStates[id].name : id; });
    alarmBanner.style.display = 'block';
    alarmBanner.innerHTML = '🔴 ' + names.map(escapeHtml).join(', ') + ' 게임 종료 <button id="bannerStopButton" class="alertStop" type="button">웹 알람 끄기</button>';
    byId('bannerStopButton').addEventListener('click', stopAllWebAlarms);
  }
  function notifyEndedRoom(room){
    activeEndedRooms.add(room.id);
    startAlarmSound();
    scheduleAutoStop();
    updateAlarmBanner();
    document.title = '🔴 ' + room.name + ' 종료!';
    if(webAlertsEnabled && 'Notification' in window && Notification.permission === 'granted'){
      try{ new Notification(room.name + ' 게임 종료',{body:'타이머가 00:00이 되었습니다.',tag:'room-end-' + room.id}); }catch(ignore){}
    }
  }
  function detectNewEndings(rooms){
    var next = {};
    rooms.forEach(function(room){
      next[room.id] = {seconds:Number(room.seconds)||0,name:room.name,running:!!room.running,status:room.status};
      var old = previousStates[room.id];
      if(old && old.seconds > 0 && Number(room.seconds) <= 0) notifyEndedRoom(room);
      if(Number(room.seconds) > 0) activeEndedRooms.delete(room.id);
    });
    previousStates = next;
    updateAlarmBanner();
  }

  async function action(roomId, actionName, seconds){
    var body = 'roomId=' + encodeURIComponent(roomId) + '&action=' + encodeURIComponent(actionName);
    if(seconds !== undefined) body += '&seconds=' + encodeURIComponent(seconds);
    try{
      var response = await fetch('/api/action', {
        method:'POST',
        headers:{'X-Manager-Pin':pin,'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'},
        body:body,
        cache:'no-store'
      });
      if(response.status === 401){ showLogin('PIN을 다시 입력해 주세요.'); return; }
      if(!response.ok) throw new Error('제어 요청 실패 (' + response.status + ')');
      window.setTimeout(refresh,100);
    }catch(error){ connection.textContent = error.message || '제어 요청 중 오류가 발생했습니다.'; }
  }
  function setTime(roomId){
    var minuteInput = byId('m_' + roomId);
    var secondInput = byId('s_' + roomId);
    var minutes = Number(minuteInput.value) || 0;
    var seconds = Math.min(59, Number(secondInput.value) || 0);
    action(roomId,'set',Math.max(0,minutes * 60 + seconds));
  }
  function card(room){
    var safeId = escapeHtml(room.id);
    var jsId = escapeJsString(room.id);
    var running = !!room.running;
    var ended = Number(room.seconds) <= 0 && !room.maintenance;
    var html = '<section class="room ' + (ended?'ended':'') + '"><div class="top"><div class="name">' + escapeHtml(room.name) + '</div><div class="badge">' + statusText(room) + '</div></div><div class="time">' + timeText(room.seconds) + '</div><div class="end">종료 예정 ' + escapeHtml(room.endLabel) + '</div>';
    if(ended){ html += '<div class="endedText">게임 종료</div><button class="alertStop room-alarm-button" data-room-id="' + safeId + '" type="button">이 방 웹 알람 확인</button>'; }
    if(room.maintenance){ html += '<div class="error">유지보수 중인 방입니다.</div>'; }
    else{
      html += '<div class="controls">'
        + '<button class="' + (running?'pause':'start') + ' room-action" data-room-id="' + safeId + '" data-action="' + (running?'pause':'start') + '" type="button">' + (running?'일시정지':'시작') + '</button>'
        + '<button class="stop room-action" data-room-id="' + safeId + '" data-action="stop" type="button">종료</button>'
        + '<button class="adjust room-action" data-room-id="' + safeId + '" data-action="adjust" data-seconds="300" type="button">+5분</button>'
        + '<button class="adjust room-action" data-room-id="' + safeId + '" data-action="adjust" data-seconds="-300" type="button">-5분</button>'
        + '<button class="room-action" data-room-id="' + safeId + '" data-action="adjust" data-seconds="10" type="button">+10초</button>'
        + '<button class="room-action" data-room-id="' + safeId + '" data-action="adjust" data-seconds="-10" type="button">-10초</button>'
        + '<button class="room-action" data-room-id="' + safeId + '" data-action="reset" type="button">초기화</button></div>'
        + '<div class="setrow"><input id="m_' + safeId + '" inputmode="numeric" placeholder="분"><input id="s_' + safeId + '" inputmode="numeric" placeholder="초"><button class="set-time-button" data-room-id="' + safeId + '" type="button">시간 적용</button></div>';
    }
    return html + '</section>';
  }
  function attachRoomEvents(){
    document.querySelectorAll('.room-action').forEach(function(button){
      button.addEventListener('click',function(){ action(button.dataset.roomId,button.dataset.action,button.dataset.seconds); });
    });
    document.querySelectorAll('.set-time-button').forEach(function(button){ button.addEventListener('click',function(){ setTime(button.dataset.roomId); }); });
    document.querySelectorAll('.room-alarm-button').forEach(function(button){ button.addEventListener('click',function(){ acknowledgeRoom(button.dataset.roomId); }); });
  }

  async function refresh(){
    if(!pin || refreshInFlight) return;
    refreshInFlight = true;
    try{
      var response = await fetch('/api/state', {headers:{'X-Manager-Pin':pin},cache:'no-store'});
      if(response.status === 401){ showLogin('PIN을 다시 입력해 주세요.'); return; }
      if(!response.ok) throw new Error('상태 조회 실패 (' + response.status + ')');
      var data = await response.json();
      detectNewEndings(data.rooms || []);
      connection.textContent = 'A7 직원용 앱과 연결됨 · 1초마다 자동 갱신';
      roomsElement.innerHTML = data.rooms && data.rooms.length ? data.rooms.map(card).join('') : '<div class="empty">사용 중인 방이 없습니다.</div>';
      attachRoomEvents();
    }catch(error){
      connection.textContent = '연결 끊김 · ' + (error && error.message ? error.message : 'A7과 같은 Wi-Fi인지 확인하세요.');
    }finally{ refreshInFlight = false; }
  }

  async function initialize(){
    loginOverlay = byId('loginOverlay');
    loginForm = byId('loginForm');
    pinInput = byId('pinInput');
    loginButton = byId('loginButton');
    loginError = byId('loginError');
    connection = byId('connection');
    roomsElement = byId('rooms');
    alarmBanner = byId('alarmBanner');
    autoStopSelect = byId('autoStopSelect');

    loginForm.addEventListener('submit',submitLogin);
    autoStopSelect.addEventListener('change',saveAlarmSettings);
    byId('enableAlertsButton').addEventListener('click',enableWebAlerts);
    byId('stopAllAlarmsButton').addEventListener('click',stopAllWebAlarms);
    autoStopSelect.value = String(autoStopSeconds);

    if(pin){
      hideLogin();
      await refresh();
      if(!pin) showLogin('저장된 PIN을 다시 확인해 주세요.');
    }else{
      showLogin('');
    }
    window.setInterval(refresh,1000);
  }

  window.addEventListener('DOMContentLoaded',function(){
    initialize().catch(function(error){
      var fallback = byId('loginError');
      if(fallback) fallback.textContent = '화면 초기화 오류: ' + (error && error.message ? error.message : error);
    });
  });
})();
