/* ============================================================
   E-Ink Launcher – Vanilla JS (no ES modules, no Proxy, no ?. )
   Compatible with Android 4.4+ WebView
   ============================================================ */
(function () {
  'use strict';

  // ---- Helpers ----
  function getJSON(url) {
    return fetch(url).then(function (res) {
      if (!res.ok) throw new Error('HTTP ' + res.status);
      return res.json();
    });
  }

  function postAction(url) {
    return fetch(url, { method: 'POST' }).then(function (res) {
      return res.json();
    });
  }

  function deleteAction(url) {
    return fetch(url, { method: 'DELETE' }).then(function (res) {
      return res.json();
    });
  }

  function toast(msg, type) {
    var el = document.createElement('div');
    el.className = 'toast toast-' + (type || 'info');
    el.textContent = msg;
    document.body.appendChild(el);
    requestAnimationFrame(function () { el.classList.add('show'); });
    setTimeout(function () {
      el.classList.remove('show');
      setTimeout(function () { el.remove(); }, 2000);
    }, 2000);
  }

  function esc(s) {
    var d = document.createElement('div');
    d.textContent = s;
    return d.innerHTML;
  }

  // ---- Upload ----
  function uploadFile(file, options) {
    var opts = options || {};
    var onProgress = opts.onProgress || null;
    var action = opts.action || 'file';
    var targetPath = opts.targetPath || '';
    var pkg = opts.pkg || '';
    var CHUNK_SIZE = 256 * 1024;

    return fetch('/api/upload/start', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        filename: file.name,
        size: file.size,
        action: action,
        targetPath: targetPath,
        pkg: pkg
      })
    }).then(function (r) { return r.json(); })
      .then(function (startData) {
        if (!startData.success) throw new Error(startData.error || 'Failed to start upload');
        var sessionId = startData.sessionId;
        var totalChunks = startData.totalChunks;
        var chunkSize = startData.chunkSize;
        var uploaded = 0;

        function sendChunk(i) {
          if (i >= totalChunks) {
            return fetch('/api/upload/complete?sessionId=' + sessionId, { method: 'POST' })
              .then(function (r) { return r.json(); });
          }
          var start = i * chunkSize;
          var end = Math.min(start + chunkSize, file.size);
          var chunk = file.slice(start, end);

          return new Promise(function (resolve, reject) {
            var xhr = new XMLHttpRequest();
            xhr.open('POST', '/api/upload/chunk?sessionId=' + sessionId + '&chunkIndex=' + i);
            xhr.setRequestHeader('Content-Type', 'application/octet-stream');
            xhr.responseType = 'json';
            xhr.onload = function () {
              if (xhr.response && xhr.response.success) {
                uploaded++;
                if (onProgress) onProgress(Math.round(uploaded / totalChunks * 100));
                resolve(sendChunk(i + 1));
              } else {
                reject(new Error(xhr.response ? xhr.response.error : 'Chunk upload failed'));
              }
            };
            xhr.onerror = function () { reject(new Error('Network error')); };
            xhr.send(chunk);
          });
        }

        return sendChunk(0);
      });
  }

  // ---- Router ----
  var currentRoute = '';
  var routes = {
    '/': { title: '首页', render: renderHome },
    '/fm': { title: '文件管理', render: renderFileManager },
    '/apk': { title: '应用管理', render: renderApkManager },
    '/icons': { title: '图标管理', render: renderIconManager },
    '/icon-gen': { title: '图标生成', render: renderIconGen },
    '/settings': { title: '系统设置', render: renderSettings }
  };

  function getRoute() {
    var hash = location.hash || '#/';
    var path = hash.replace(/^#/, '') || '/';
    var qIdx = path.indexOf('?');
    var routePath = qIdx >= 0 ? path.substring(0, qIdx) : path;
    var query = qIdx >= 0 ? path.substring(qIdx + 1) : '';
    return { path: routePath, query: query };
  }

  function navigate() {
    var r = getRoute();
    var routeObj = routes[r.path];
    if (!routeObj) routeObj = routes['/'];
    currentRoute = r.path;

    document.title = routeObj.title + ' · E-Ink Launcher';
    var content = document.getElementById('content');
    content.innerHTML = '<div class="loading">加载中…</div>';

    routeObj.render(content, r);

    // Update active tab
    var links = document.querySelectorAll('.tab');
    for (var i = 0; i < links.length; i++) {
      var link = links[i];
      var linkRoute = link.getAttribute('data-route');
      if (linkRoute === currentRoute) {
        link.classList.add('active');
      } else {
        link.classList.remove('active');
      }
    }
  }

  // ---- Home View ----
  function renderHome(content) {
    var gearPath = '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0 1.82.33H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>';
    content.innerHTML = '<div class="bento-grid">' +
      '<div class="card"><div class="card-title">文件</div><div class="card-value" id="home-file-count">—</div></div>' +
      '<div class="card"><div class="card-title">存储</div><div class="card-value" id="home-storage" style="font-size:18px;">—</div><div class="progress-bar"><div class="progress-fill" id="home-storage-bar" style="width:0"></div></div></div>' +
      '<div class="card"><div class="card-title">电池</div><div class="card-value" id="home-battery">—</div></div>' +
      '<div class="card"><div class="card-title">WiFi</div><div class="card-value" id="home-wifi" style="font-size:16px;">—</div></div>' +
      '<div class="card"><div class="card-title">服务器</div><div class="card-value" style="font-size:15px;word-break:break-all;" id="home-server">—</div></div>' +
      '</div>' +
      '<div class="grid">' +
      '<div class="grid-card" onclick="location.hash=\'#/fm\'"><svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg><div class="label">文件管理</div><div class="desc">浏览和管理文件</div></div>' +
      '<div class="grid-card" onclick="location.hash=\'#/apk\'"><svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="5" y="2" width="14" height="20" rx="2"/><line x1="12" y1="18" x2="12.01" y2="18"/></svg><div class="label">APK 管理</div><div class="desc">安装和管理应用</div></div>' +
      '<div class="grid-card" onclick="location.hash=\'#/icons\'"><svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg><div class="label">图标管理</div><div class="desc">自定义应用图标</div></div>' +
      '<div class="grid-card" onclick="location.hash=\'#/icon-gen\'"><svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 14c.2-1 .7-1.7 1.5-2.5 1-.9 1.5-2.2 1.5-3.5A6 6 0 0 0 6 8c0 1 .2 2.2 1.5 3.5.7.7 1.3 1.5 1.5 2.5"/><path d="M9 18h6"/><path d="M10 22h4"/></svg><div class="label">图标生成</div><div class="desc">文字/图片生成图标</div></div>' +
      '<div class="grid-card" onclick="location.hash=\'#/settings\'"><svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">' + gearPath + '</svg><div class="label">系统设置</div><div class="desc">设备参数调节</div></div>' +
      '<div class="grid-card" id="home-refresh"><svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg><div class="label">刷新状态</div><div class="desc">重新获取设备信息</div></div>' +
      '</div>';

    function loadHomeStats() {
      Promise.all([
        getJSON('/api/stats').catch(function () { return {}; }),
        getJSON('/api/battery').catch(function () { return {}; }),
        getJSON('/api/wifi-status').catch(function () { return {}; }),
        getJSON('/api/storage').catch(function () { return {}; })
      ]).then(function (results) {
        var s = results[0], b = results[1], w = results[2], st = results[3];
        var fc = document.getElementById('home-file-count');
        if (fc) fc.textContent = s.fileCount || '0';
        var storage = document.getElementById('home-storage');
        if (storage) storage.textContent = (st.usedHuman || '—') + ' / ' + (st.totalHuman || '—');
        var bar = document.getElementById('home-storage-bar');
        if (bar && st.total) bar.style.width = Math.round((st.used || 0) / st.total * 100) + '%';
        var bat = document.getElementById('home-battery');
        if (bat) bat.textContent = (b.level || '0') + '%';
        var wifi = document.getElementById('home-wifi');
        if (wifi) wifi.textContent = w.ssid || '未连接';
        var srv = document.getElementById('home-server');
        if (srv) srv.textContent = (location.origin && location.origin !== 'null' && location.origin !== 'file://') ? location.origin : '—';
      });
    }

    var refreshTile = document.getElementById('home-refresh');
    if (refreshTile) {
      refreshTile.addEventListener('click', function () {
        loadHomeStats();
        toast('已刷新', 'info');
      });
    }
    loadHomeStats();
  }

  // ---- File Manager View ----
  function renderFileManager(content, route) {
    var currentPath = '/sdcard';
    var qParts = route.query.split('=');
    for (var i = 0; i < qParts.length - 1; i += 2) {
      if (qParts[i] === 'path') currentPath = decodeURIComponent(qParts[i + 1] || '/sdcard');
    }

    content.innerHTML = '<div class="card" style="font-size:12px;color:var(--text-muted);"><div style="display:flex;align-items:center;gap:6px;"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg><span>' + esc(currentPath) + '</span></div></div>' +
      '<div class="card" style="padding:0;" id="fm-list"><div class="loading">加载中…</div></div>' +
      '<div class="upload-area" id="uploadZone" style="margin-top:12px;"><svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg><div style="font-size:13px;font-weight:500;">点击或拖拽上传文件</div><input type="file" multiple id="fm-fileInput" style="display:none;" /><div id="fm-upload-progress" style="display:none;" class="upload-progress"><div class="upload-progress-bar" id="fm-upload-bar"></div><div class="upload-progress-text" id="fm-upload-text"></div></div></div>';

    var listEl = document.getElementById('fm-list');
    var fileInput = document.getElementById('fm-fileInput');
    var uploadZone = document.getElementById('uploadZone');

    function loadFiles() {
      listEl.innerHTML = '<div class="loading">加载中…</div>';
      getJSON('/api/files?path=' + encodeURIComponent(currentPath))
        .then(function (data) {
          var items = data.items || [];
          var parentPath = data.parentPath || '';
          var html = '';

          if (parentPath && parentPath !== currentPath) {
            html += '<div class="row" style="text-decoration:none;color:var(--text);cursor:pointer;" onclick="location.hash=\'#/fm?path=' + encodeURIComponent(parentPath) + '\'"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg><div class="row-text"><span class="row-title" style="color:var(--primary);">..</span></div></div>';
          }

          for (var i = 0; i < items.length; i++) {
            var f = items[i];
            if (f.isDir) {
              html += '<div class="row"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg><div class="row-text"><span class="row-title" style="cursor:pointer;color:var(--primary);" onclick="location.hash=\'#/fm?path=' + encodeURIComponent(f.path) + '\'">' + esc(f.name) + '</span></div><div class="row-action"><button class="btn btn-sm btn-danger" data-delete="' + esc(f.path) + '" data-name="' + esc(f.name) + '">删除</button></div></div>';
            } else {
              html += '<div class="row"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" stroke-width="2"><path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"/><polyline points="13 2 13 9 20 9"/></svg><div class="row-text"><a href="' + esc(f.path) + '" download class="row-title" style="text-decoration:none;">' + esc(f.name) + '</a><div class="row-sub">' + esc(f.sizeHuman || '') + '</div></div><div class="row-action"><button class="btn btn-sm btn-danger" data-delete="' + esc(f.path) + '" data-name="' + esc(f.name) + '">删除</button></div></div>';
            }
          }

          if (items.length === 0) html = '<div class="empty-state">空目录</div>';
          listEl.innerHTML = html;

          var deleteButtons = listEl.querySelectorAll('[data-delete]');
          for (var j = 0; j < deleteButtons.length; j++) {
            deleteButtons[j].addEventListener('click', function () {
              var dp = this.getAttribute('data-delete');
              var dn = this.getAttribute('data-name');
              if (!confirm('删除 "' + dn + '"?')) return;
              deleteAction('/api/files?path=' + encodeURIComponent(dp))
                .then(function (r) {
                  toast(r.success ? '已删除' : '删除失败', r.success ? 'success' : 'error');
                  if (r.success) loadFiles();
                })
                .catch(function () { toast('删除失败', 'error'); });
            });
          }
        })
        .catch(function () { listEl.innerHTML = '<div class="empty-state">加载失败</div>'; });
    }

    fileInput.addEventListener('change', function () {
      var files = this.files;
      if (!files.length) return;
      uploadFiles(files);
    });

    uploadZone.addEventListener('click', function (e) {
      if (e.target.tagName !== 'INPUT') fileInput.click();
    });

    uploadZone.addEventListener('dragover', function (e) { e.preventDefault(); });
    uploadZone.addEventListener('drop', function (e) {
      e.preventDefault();
      var files = e.dataTransfer.files;
      if (files.length) uploadFiles(files);
    });

    function uploadFiles(files) {
      var progressEl = document.getElementById('fm-upload-progress');
      var barEl = document.getElementById('fm-upload-bar');
      var textEl = document.getElementById('fm-upload-text');
      var idx = 0;

      function next() {
        if (idx >= files.length) {
          progressEl.style.display = 'none';
          loadFiles();
          return;
        }
        var file = files[idx];
        idx++;
        progressEl.style.display = '';
        barEl.style.width = '0%';
        textEl.textContent = '0%';

        uploadFile(file, {
          targetPath: currentPath,
          onProgress: function (pct) {
            barEl.style.width = pct + '%';
            textEl.textContent = pct + '%';
          }
        }).then(function () {
          toast('已上传: ' + file.name, 'success');
          next();
        }).catch(function () {
          toast('上传失败: ' + file.name, 'error');
          next();
        });
      }
      next();
    }

    loadFiles();
  }

  // ---- APK Manager View ----
  function renderApkManager(content) {
    content.innerHTML = '<div class="search-bar"><input class="form-input" id="apk-search" placeholder="搜索应用…" /></div>' +
      '<div class="app-grid" id="apk-grid"><div class="loading">加载中…</div></div>' +
      '<div class="card" style="margin-top:16px;"><div class="card-title">安装 APK</div><div class="upload-area" id="apk-uploadArea"><svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" stroke-width="1.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg><div style="font-size:13px;color:var(--text-muted);">点击或拖拽 APK 文件</div><input type="file" accept=".apk" id="apk-fileInput" style="display:none;" /><div id="apk-upload-progress" style="display:none;" class="upload-progress"><div class="upload-progress-bar" id="apk-upload-bar"></div><div class="upload-progress-text" id="apk-upload-text"></div></div></div></div>';

    var gridEl = document.getElementById('apk-grid');
    var searchEl = document.getElementById('apk-search');
    var fileInput = document.getElementById('apk-fileInput');
    var uploadArea = document.getElementById('apk-uploadArea');
    var allApps = [];

    function loadApps() {
      gridEl.innerHTML = '<div class="loading">加载中…</div>';
      getJSON('/api/apps').then(function (data) {
        allApps = data.items || [];
        filterApps('');
      }).catch(function () { gridEl.innerHTML = '<div class="empty-state">加载失败</div>'; });
    }

    function filterApps(q) {
      var lower = q.toLowerCase();
      var list = allApps;
      if (lower) {
        list = [];
        for (var i = 0; i < allApps.length; i++) {
          var a = allApps[i];
          if (a.name.toLowerCase().indexOf(lower) >= 0 || a.packageName.toLowerCase().indexOf(lower) >= 0) {
            list.push(a);
          }
        }
      }
      var html = '';
      for (var j = 0; j < list.length; j++) {
        var app = list[j];
        html += '<div class="app-item"><img class="app-icon" src="/api/app-icon?pkg=' + encodeURIComponent(app.packageName) + '" onerror="this.style.display=\'none\'" /><div class="app-name">' + esc(app.name) + '</div><div class="app-pkg">' + esc(app.packageName) + '</div>';
        if (!app.isVirtual) {
          html += '<div class="app-actions"><button class="btn btn-sm apk-open" data-pkg="' + esc(app.packageName) + '">打开</button><button class="btn btn-sm btn-danger apk-uninstall" data-pkg="' + esc(app.packageName) + '" data-name="' + esc(app.name) + '">卸载</button></div>';
        }
        html += '</div>';
      }
      if (list.length === 0) html = '<div class="empty-state">暂无应用</div>';
      gridEl.innerHTML = html;

      var openBtns = gridEl.querySelectorAll('.apk-open');
      for (var k = 0; k < openBtns.length; k++) {
        openBtns[k].addEventListener('click', function () {
          var pkg = this.getAttribute('data-pkg');
          postAction('/api/app-open?pkg=' + encodeURIComponent(pkg)).then(function (d) {
            toast(d.success ? '已打开' : (d.error || '失败'), d.success ? 'success' : 'error');
          }).catch(function () { toast('失败', 'error'); });
        });
      }

      var uninstallBtns = gridEl.querySelectorAll('.apk-uninstall');
      for (var m = 0; m < uninstallBtns.length; m++) {
        uninstallBtns[m].addEventListener('click', function () {
          var pkg = this.getAttribute('data-pkg');
          var name = this.getAttribute('data-name');
          if (!confirm('卸载 ' + name + '?')) return;
          postAction('/api/app-uninstall?pkg=' + encodeURIComponent(pkg)).then(function (d) {
            toast(d.success ? '卸载已开始' : (d.error || '失败'), d.success ? 'success' : 'error');
          }).catch(function () { toast('失败', 'error'); });
        });
      }
    }

    searchEl.addEventListener('input', function () { filterApps(this.value); });

    fileInput.addEventListener('change', function () {
      if (this.files.length) installApk(this.files[0]);
    });

    uploadArea.addEventListener('click', function (e) {
      if (e.target.tagName !== 'INPUT') fileInput.click();
    });

    uploadArea.addEventListener('dragover', function (e) { e.preventDefault(); });
    uploadArea.addEventListener('drop', function (e) {
      e.preventDefault();
      if (e.dataTransfer.files.length) installApk(e.dataTransfer.files[0]);
    });

    function installApk(file) {
      var progressEl = document.getElementById('apk-upload-progress');
      var barEl = document.getElementById('apk-upload-bar');
      var textEl = document.getElementById('apk-upload-text');
      progressEl.style.display = '';
      barEl.style.width = '0%';
      textEl.textContent = '0%';

      uploadFile(file, {
        targetPath: '/sdcard/Download',
        action: 'install',
        onProgress: function (pct) {
          barEl.style.width = pct + '%';
          textEl.textContent = pct + '%';
        }
      }).then(function (r) {
        return postAction('/api/app-install?path=' + encodeURIComponent(r.path));
      }).then(function (r2) {
        toast(r2.success ? '安装已开始' : (r2.error || '失败'), r2.success ? 'success' : 'error');
        progressEl.style.display = 'none';
      }).catch(function () {
        toast('上传/安装失败', 'error');
        progressEl.style.display = 'none';
      });
    }

    loadApps();
  }

  // ---- Icon Manager View ----
  function renderIconManager(content) {
    content.innerHTML = '<div class="search-bar"><input class="form-input" id="icon-search" placeholder="搜索应用…" /></div>' +
      '<div style="margin-bottom:12px;"><a class="btn btn-primary" href="#/icon-gen" style="width:100%;display:flex;align-items:center;justify-content:center;gap:8px;text-decoration:none;"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="16"/><line x1="8" y1="12" x2="16" y2="12"/></svg>圆角图片生成器</a></div>' +
      '<div class="icon-grid" id="icon-grid"><div class="loading">加载中…</div></div>';

    var gridEl = document.getElementById('icon-grid');
    var searchEl = document.getElementById('icon-search');
    var allIcons = [];

    function loadIcons() {
      gridEl.innerHTML = '<div class="loading">加载中…</div>';
      getJSON('/api/icons').then(function (data) {
        allIcons = data.items || [];
        filterIcons('');
      }).catch(function () { gridEl.innerHTML = '<div class="empty-state">加载失败</div>'; });
    }

    function filterIcons(q) {
      var lower = q.toLowerCase();
      var list = allIcons;
      if (lower) {
        list = [];
        for (var i = 0; i < allIcons.length; i++) {
          var a = allIcons[i];
          if (a.name.toLowerCase().indexOf(lower) >= 0 || a.packageName.toLowerCase().indexOf(lower) >= 0) {
            list.push(a);
          }
        }
      }
      var html = '';
      for (var j = 0; j < list.length; j++) {
        var item = list[j];
        html += '<div class="icon-slot" data-pkg="' + esc(item.packageName) + '">';
        html += '<img src="/api/app-icon?pkg=' + encodeURIComponent(item.packageName) + '" style="width:100%;height:100%;object-fit:cover;border-radius:12px;position:absolute;top:0;left:0;" />';
        html += '<div class="icon-slot-name">' + esc(item.name) + '</div>';
        if (item.hasCustomIcon) html += '<div class="icon-slot-badge">Custom</div>';
        html += '<input type="file" accept="image/*" class="icon-file-input" data-pkg="' + esc(item.packageName) + '" style="display:none;" />';
        html += '</div>';
      }
      if (list.length === 0) html = '<div class="empty-state">暂无应用</div>';
      gridEl.innerHTML = html;

      var slots = gridEl.querySelectorAll('.icon-slot');
      for (var k = 0; k < slots.length; k++) {
        slots[k].addEventListener('click', function (e) {
          if (e.target.classList.contains('icon-file-input')) return;
          var pkg = this.getAttribute('data-pkg');
          var inp = this.querySelector('.icon-file-input');
          if (inp) inp.click();
        });
      }

      var fileInputs = gridEl.querySelectorAll('.icon-file-input');
      for (var m = 0; m < fileInputs.length; m++) {
        fileInputs[m].addEventListener('change', function (e) {
          var file = e.target.files[0];
          if (!file) return;
          var pkg = this.getAttribute('data-pkg');
          uploadFile(file, { action: 'icon-replace', pkg: pkg }).then(function () {
            toast('图标已上传', 'success');
            loadIcons();
          }).catch(function () { toast('上传失败', 'error'); });
        });
      }

      for (var n = 0; n < slots.length; n++) {
        (function (slot) {
          slot.addEventListener('dragover', function (e) { e.preventDefault(); });
          slot.addEventListener('drop', function (e) {
            e.preventDefault();
            var pkg = this.getAttribute('data-pkg');
            var file = e.dataTransfer.files[0];
            if (!file) return;
            uploadFile(file, { action: 'icon-replace', pkg: pkg }).then(function () {
              toast('图标已上传', 'success');
              loadIcons();
            }).catch(function () { toast('上传失败', 'error'); });
          });
        })(slots[n]);
      }
    }

    searchEl.addEventListener('input', function () { filterIcons(this.value); });
    loadIcons();
  }

  // ---- Icon Generator View ----
  function renderIconGen(content) {
    content.innerHTML =
      '<div class="card" style="text-align:center;padding:16px;"><canvas id="igen-canvas" width="256" height="256" style="max-width:256px;max-height:256px;border-radius:12px;"></canvas></div>' +
      '<div class="control-section"><div class="control-section-title">图标内容</div>' +
      '<div style="display:flex;gap:8px;"><button class="btn btn-primary" id="igen-mode-text">文字</button><button class="btn" id="igen-mode-image">图片</button></div>' +
      '<div id="igen-text-input" style="margin-top:12px;"><input class="form-input" id="igen-text" maxlength="2" placeholder="输入 1~2 个字" value="设置" /></div>' +
      '<div id="igen-image-input" style="margin-top:12px;display:none;"><div class="upload-area" id="igen-uploadArea"><svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" stroke-width="1.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg><div style="font-size:13px;color:var(--text-muted);margin-top:8px;">点击或拖拽上传图片</div><div style="font-size:12px;color:var(--text-muted);margin-top:4px;">支持 PNG / JPG / WebP</div><input type="file" accept="image/*" id="igen-imageFile" style="display:none;" /></div></div>' +
      '</div>' +
      '<div class="control-section"><div class="control-section-title">配色方案</div><div class="color-presets" id="igen-schemes"></div>' +
      '<div style="margin-top:12px;"><div class="color-row"><input type="color" id="igen-bg" value="#ffffff" /><div style="flex:1;min-width:0;"><div class="color-hex" id="igen-bg-hex">#FFFFFF</div><div class="color-label">背景色</div></div></div>' +
      '<div class="color-row" style="margin-top:8px;"><input type="color" id="igen-fg" value="#1a1a1a" /><div style="flex:1;min-width:0;"><div class="color-hex" id="igen-fg-hex">#1A1A1A</div><div class="color-label">文字色</div></div></div></div></div>' +
      '<div class="control-section"><div class="control-section-title">边框</div>' +
      '<div style="display:flex;align-items:center;justify-content:space-between;"><span style="font-size:14px;font-weight:500;">显示边框（跟随文字色）</span><div class="toggle active" id="igen-border-toggle"></div></div>' +
      '<div style="margin-top:12px;"><div style="display:flex;justify-content:space-between;margin-bottom:6px;"><span style="font-size:13px;color:var(--text-muted);">边框宽度</span><span style="font-size:13px;color:var(--text-muted);" id="igen-borderWidth-label">3 px</span></div><input type="range" id="igen-borderWidth" min="1" max="12" value="3" style="width:100%;height:6px;" /></div></div>' +
      '<div class="control-section"><div class="control-section-title">字体</div><div style="display:flex;gap:8px;flex-wrap:wrap;" id="igen-fonts"></div>' +
      '<div style="margin-top:12px;"><div style="font-size:13px;color:var(--text-muted);margin-bottom:8px;">字体粗细</div><div style="display:flex;gap:8px;" id="igen-weights"></div></div></div>' +
      '<div class="control-section"><div class="control-section-title">尺寸与样式</div>' +
      '<div style="margin-bottom:10px;"><div style="display:flex;justify-content:space-between;margin-bottom:4px;"><span style="font-size:13px;color:var(--text-muted);">图片尺寸</span><span style="font-size:13px;color:var(--text-muted);" id="igen-size-label">256 px</span></div><input type="range" id="igen-size" min="64" max="1024" value="256" style="width:100%;height:6px;" /></div>' +
      '<div style="margin-bottom:10px;"><div style="display:flex;justify-content:space-between;margin-bottom:4px;"><span style="font-size:13px;color:var(--text-muted);">圆角半径</span><span style="font-size:13px;color:var(--text-muted);" id="igen-radius-label">32 px</span></div><input type="range" id="igen-radius" min="0" max="128" value="32" style="width:100%;height:6px;" /></div>' +
      '<div style="margin-bottom:10px;"><div style="display:flex;justify-content:space-between;margin-bottom:4px;"><span style="font-size:13px;color:var(--text-muted);">字号</span><span style="font-size:13px;color:var(--text-muted);" id="igen-fontSize-label">100 %</span></div><input type="range" id="igen-fontSize" min="30" max="200" value="100" style="width:100%;height:6px;" /></div>' +
      '<div style="margin-bottom:10px;"><div style="display:flex;justify-content:space-between;margin-bottom:4px;"><span style="font-size:13px;color:var(--text-muted);">字间距</span><span style="font-size:13px;color:var(--text-muted);" id="igen-letterSpacing-label">0 px</span></div><input type="range" id="igen-letterSpacing" min="-10" max="30" value="0" style="width:100%;height:6px;" /></div>' +
      '</div>' +
      '<div style="display:flex;gap:8px;"><button class="btn" id="igen-copy" style="flex:1;padding:12px;border-radius:8px;border:2px solid var(--border);background:var(--bg);font-size:14px;font-weight:600;min-height:48px;display:flex;align-items:center;justify-content:center;gap:8px;"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>复制</button>' +
      '<button class="btn btn-primary" id="igen-download" style="flex:1;padding:12px;border-radius:8px;font-size:14px;font-weight:600;min-height:48px;display:flex;align-items:center;justify-content:center;gap:8px;"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>下载 PNG</button></div>';

    var canvas = document.getElementById('igen-canvas');
    var ctx = canvas.getContext('2d');
    var mode = 'text';
    var text = '设置';
    var bgColor = '#ffffff';
    var fgColor = '#1a1a1a';
    var showBorder = true;
    var borderWidth = 3;
    var fontFamily = 'system-ui';
    var fontWeight = '700';
    var sizeVal = 256;
    var radiusVal = 32;
    var fontSizeVal = 100;
    var letterSpacingVal = 0;
    var uploadedImg = null;

    var colorSchemes = [
      { label: '白底黑字', bg: '#ffffff', fg: '#1a1a1a' },
      { label: '黑底白字', bg: '#1a1a1a', fg: '#ffffff' },
      { label: '蓝底白字', bg: '#1a73e8', fg: '#ffffff' },
      { label: '绿底白字', bg: '#1e8e3e', fg: '#ffffff' },
      { label: '红底白字', bg: '#d93025', fg: '#ffffff' },
      { label: '黄底黑字', bg: '#f9ab00', fg: '#1a1a1a' }
    ];

    var fonts = [
      { label: '默认', value: 'system-ui' },
      { label: '宋体', value: 'serif' },
      { label: '黑体', value: 'sans-serif' },
      { label: '楷体', value: 'cursive' },
      { label: '等宽', value: 'monospace' }
    ];

    var weights = ['400', '700', '900'];
    var weightLabels = { '400': 'Regular', '700': 'Bold', '900': 'Black' };

    // Build scheme buttons
    var schemesHtml = '';
    for (var si = 0; si < colorSchemes.length; si++) {
      var s = colorSchemes[si];
      schemesHtml += '<button class="color-scheme-btn' + (s.label === '白底黑字' ? ' active' : '') + '" data-scheme="' + si + '">' + s.label + '</button>';
    }
    document.getElementById('igen-schemes').innerHTML = schemesHtml;

    // Build font buttons
    var fontsHtml = '';
    for (var fi = 0; fi < fonts.length; fi++) {
      var f = fonts[fi];
      fontsHtml += '<button class="btn btn-sm' + (f.value === 'system-ui' ? ' btn-primary' : '') + '" data-font="' + f.value + '">' + f.label + '</button>';
    }
    document.getElementById('igen-fonts').innerHTML = fontsHtml;

    // Build weight buttons
    var weightsHtml = '';
    for (var wi = 0; wi < weights.length; wi++) {
      var w = weights[wi];
      weightsHtml += '<button class="btn btn-sm' + (w === '700' ? ' btn-primary' : '') + '" data-weight="' + w + '">' + weightLabels[w] + '</button>';
    }
    document.getElementById('igen-weights').innerHTML = weightsHtml;

    function draw() {
      var s = sizeVal;
      canvas.width = s;
      canvas.height = s;
      var r = Math.min(radiusVal, s / 2);
      ctx.clearRect(0, 0, s, s);

      // background
      ctx.beginPath();
      ctx.moveTo(r, 0);
      ctx.lineTo(s - r, 0);
      ctx.quadraticCurveTo(s, 0, s, r);
      ctx.lineTo(s, s - r);
      ctx.quadraticCurveTo(s, s, s - r, s);
      ctx.lineTo(r, s);
      ctx.quadraticCurveTo(0, s, 0, s - r);
      ctx.lineTo(0, r);
      ctx.quadraticCurveTo(0, 0, r, 0);
      ctx.closePath();
      ctx.fillStyle = bgColor;
      ctx.fill();

      // border
      if (showBorder && borderWidth > 0) {
        var bw = borderWidth;
        ctx.save();
        ctx.beginPath();
        var br = Math.max(0, r - bw / 2);
        ctx.moveTo(br + bw / 2, bw / 2);
        ctx.lineTo(s - br - bw / 2, bw / 2);
        ctx.quadraticCurveTo(s - bw / 2, bw / 2, s - bw / 2, br + bw / 2);
        ctx.lineTo(s - bw / 2, s - br - bw / 2);
        ctx.quadraticCurveTo(s - bw / 2, s - bw / 2, s - br - bw / 2, s - bw / 2);
        ctx.lineTo(br + bw / 2, s - bw / 2);
        ctx.quadraticCurveTo(bw / 2, s - bw / 2, bw / 2, s - br - bw / 2);
        ctx.lineTo(bw / 2, br + bw / 2);
        ctx.quadraticCurveTo(bw / 2, bw / 2, br + bw / 2, bw / 2);
        ctx.closePath();
        ctx.strokeStyle = fgColor;
        ctx.lineWidth = bw;
        ctx.stroke();
        ctx.restore();
      }

      // image mode
      if (mode === 'image' && uploadedImg) {
        var scale = Math.min(s / uploadedImg.width, s / uploadedImg.height);
        var iw = uploadedImg.width * scale;
        var ih = uploadedImg.height * scale;
        ctx.drawImage(uploadedImg, (s - iw) / 2, (s - ih) / 2, iw, ih);
        return;
      }

      // text mode
      var t = text;
      if (!t) return;
      var charCount = t.length;
      var baseFs = s * 0.42 * (fontSizeVal / 100);
      var fs = charCount > 1 ? baseFs * 0.85 : baseFs;
      ctx.fillStyle = fgColor;
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.font = fontWeight + ' ' + fs + 'px ' + fontFamily;
      var sp = letterSpacingVal;
      if (charCount === 1) {
        ctx.fillText(t, s / 2, s / 2);
      } else {
        var chars = t.split('');
        var widths = [];
        for (var ci = 0; ci < chars.length; ci++) {
          widths.push(ctx.measureText(chars[ci]).width);
        }
        var totalW = 0;
        for (var wi2 = 0; wi2 < widths.length; wi2++) totalW += widths[wi2];
        totalW += sp * (charCount - 1);
        var cx = (s - totalW) / 2;
        for (var cj = 0; cj < chars.length; cj++) {
          ctx.fillText(chars[cj], cx + widths[cj] / 2, s / 2);
          cx += widths[cj] + sp;
        }
      }
    }

    // Mode toggle
    document.getElementById('igen-mode-text').addEventListener('click', function () {
      mode = 'text';
      this.classList.add('btn-primary');
      document.getElementById('igen-mode-image').classList.remove('btn-primary');
      document.getElementById('igen-text-input').style.display = '';
      document.getElementById('igen-image-input').style.display = 'none';
      draw();
    });
    document.getElementById('igen-mode-image').addEventListener('click', function () {
      mode = 'image';
      this.classList.add('btn-primary');
      document.getElementById('igen-mode-text').classList.remove('btn-primary');
      document.getElementById('igen-text-input').style.display = 'none';
      document.getElementById('igen-image-input').style.display = '';
      draw();
    });

    document.getElementById('igen-text').addEventListener('input', function () {
      text = this.value;
      draw();
    });

    // Scheme buttons
    var schemeBtns = document.querySelectorAll('[data-scheme]');
    for (var si2 = 0; si2 < schemeBtns.length; si2++) {
      schemeBtns[si2].addEventListener('click', function () {
        var idx = parseInt(this.getAttribute('data-scheme'));
        var scheme = colorSchemes[idx];
        bgColor = scheme.bg;
        fgColor = scheme.fg;
        document.getElementById('igen-bg').value = bgColor;
        document.getElementById('igen-fg').value = fgColor;
        document.getElementById('igen-bg-hex').textContent = bgColor.toUpperCase();
        document.getElementById('igen-fg-hex').textContent = fgColor.toUpperCase();
        var allSchemeBtns = document.querySelectorAll('[data-scheme]');
        for (var x = 0; x < allSchemeBtns.length; x++) allSchemeBtns[x].classList.remove('active');
        this.classList.add('active');
        draw();
      });
    }

    // Color pickers
    document.getElementById('igen-bg').addEventListener('input', function () {
      bgColor = this.value;
      document.getElementById('igen-bg-hex').textContent = bgColor.toUpperCase();
      var allSchemeBtns = document.querySelectorAll('[data-scheme]');
      for (var x = 0; x < allSchemeBtns.length; x++) allSchemeBtns[x].classList.remove('active');
      draw();
    });
    document.getElementById('igen-fg').addEventListener('input', function () {
      fgColor = this.value;
      document.getElementById('igen-fg-hex').textContent = fgColor.toUpperCase();
      var allSchemeBtns = document.querySelectorAll('[data-scheme]');
      for (var x = 0; x < allSchemeBtns.length; x++) allSchemeBtns[x].classList.remove('active');
      draw();
    });

    // Border toggle
    document.getElementById('igen-border-toggle').addEventListener('click', function () {
      showBorder = !showBorder;
      this.classList.toggle('active', showBorder);
      draw();
    });

    // Border width
    document.getElementById('igen-borderWidth').addEventListener('input', function () {
      borderWidth = parseInt(this.value);
      document.getElementById('igen-borderWidth-label').textContent = borderWidth + ' px';
      draw();
    });

    // Font buttons
    var fontBtns = document.querySelectorAll('[data-font]');
    for (var fi2 = 0; fi2 < fontBtns.length; fi2++) {
      fontBtns[fi2].addEventListener('click', function () {
        fontFamily = this.getAttribute('data-font');
        var allFontBtns = document.querySelectorAll('[data-font]');
        for (var x = 0; x < allFontBtns.length; x++) {
          allFontBtns[x].classList.remove('btn-primary');
          allFontBtns[x].classList.add('btn');
        }
        this.classList.add('btn-primary');
        draw();
      });
    }

    // Weight buttons
    var weightBtns = document.querySelectorAll('[data-weight]');
    for (var wi3 = 0; wi3 < weightBtns.length; wi3++) {
      weightBtns[wi3].addEventListener('click', function () {
        fontWeight = this.getAttribute('data-weight');
        var allWeightBtns = document.querySelectorAll('[data-weight]');
        for (var x = 0; x < allWeightBtns.length; x++) {
          allWeightBtns[x].classList.remove('btn-primary');
          allWeightBtns[x].classList.add('btn');
        }
        this.classList.add('btn-primary');
        draw();
      });
    }

    // Size sliders
    var sliderMap = {
      'igen-size': { key: 'size', label: 'igen-size-label' },
      'igen-radius': { key: 'radius', label: 'igen-radius-label' },
      'igen-fontSize': { key: 'fontSize', label: 'igen-fontSize-label' },
      'igen-letterSpacing': { key: 'letterSpacing', label: 'igen-letterSpacing-label' }
    };
    var sliderKeys = Object.keys(sliderMap);
    for (var sli = 0; sli < sliderKeys.length; sli++) {
      (function (sliderId) {
        var slider = document.getElementById(sliderId);
        if (!slider) return;
        slider.addEventListener('input', function () {
          var val = parseInt(this.value);
          var info = sliderMap[sliderId];
          if (info.key === 'size') sizeVal = val;
          else if (info.key === 'radius') radiusVal = val;
          else if (info.key === 'fontSize') fontSizeVal = val;
          else if (info.key === 'letterSpacing') letterSpacingVal = val;
          document.getElementById(info.label).textContent = val + ' px';
          draw();
        });
      })(sliderKeys[sli]);
    }

    // Image upload
    var imageFileInput = document.getElementById('igen-imageFile');
    var uploadArea = document.getElementById('igen-uploadArea');

    imageFileInput.addEventListener('change', function () {
      if (this.files[0]) loadImageFile(this.files[0]);
    });
    uploadArea.addEventListener('click', function (e) {
      if (e.target.tagName !== 'INPUT') imageFileInput.click();
    });
    uploadArea.addEventListener('dragover', function (e) { e.preventDefault(); });
    uploadArea.addEventListener('drop', function (e) {
      e.preventDefault();
      if (e.dataTransfer.files[0]) loadImageFile(e.dataTransfer.files[0]);
    });

    function loadImageFile(file) {
      if (file.type.indexOf('image/') !== 0) {
        toast('请上传图片文件', 'error');
        return;
      }
      var reader = new FileReader();
      reader.onload = function (e) {
        var img = new Image();
        img.onload = function () {
          uploadedImg = img;
          draw();
          toast('图片已加载', 'success');
        };
        img.src = e.target.result;
      };
      reader.readAsDataURL(file);
    }

    // Copy & Download
    document.getElementById('igen-copy').addEventListener('click', function () {
      canvas.toBlob(function (blob) {
        if (navigator.clipboard && navigator.clipboard.write) {
          var item = new ClipboardItem({ 'image/png': blob });
          navigator.clipboard.write([item]).then(function () {
            toast('已复制到剪贴板', 'success');
          }).catch(function () {
            toast('复制失败，请重试', 'error');
          });
        } else {
          toast('复制失败，请重试', 'error');
        }
      }, 'image/png');
    });

    document.getElementById('igen-download').addEventListener('click', function () {
      var a = document.createElement('a');
      a.download = (mode === 'image' ? 'image' : (text || 'icon')) + '.png';
      a.href = canvas.toDataURL('image/png');
      a.click();
      toast('已保存到下载目录', 'success');
    });

    draw();
  }

  // ---- Settings View ----
  function renderSettings(content) {
    content.innerHTML = '<div class="bento-grid">' +
      '<div class="card"><div class="card-title">设备</div><div style="font-size:14px;font-weight:600;" id="set-device">—</div><div style="font-size:12px;color:var(--text-muted);" id="set-device-info">—</div></div>' +
      '<div class="card"><div class="card-title">电池</div><div style="display:flex;align-items:center;gap:8px;"><div class="card-value" style="font-size:18px;" id="set-battery">—</div><div style="flex:1;"><div class="progress-bar"><div class="progress-fill" id="set-battery-bar" style="width:0"></div></div></div></div><div style="font-size:11px;color:var(--text-muted);margin-top:4px;" id="set-battery-info">—</div></div>' +
      '<div class="card"><div class="card-title">存储</div><div class="card-value" style="font-size:16px;" id="set-storage">—</div><div class="progress-bar" style="margin-top:6px;"><div class="progress-fill" id="set-storage-bar" style="width:0"></div></div><div style="font-size:11px;color:var(--text-muted);margin-top:4px;" id="set-storage-info">—</div></div>' +
      '<div class="card"><div class="card-title">WiFi</div><div class="card-value" style="font-size:16px;" id="set-wifi">—</div><div style="font-size:11px;color:var(--text-muted);" id="set-wifi-info">—</div></div>' +
      '</div>' +
      '<h2 style="margin:20px 0 10px;font-size:15px;">音量</h2><div class="card" id="set-volumes"></div>' +
      '<h2 style="margin:20px 0 10px;font-size:15px;">亮度</h2><div class="card" id="set-brightness"></div>' +
      '<h2 style="margin:20px 0 10px;font-size:15px;">屏幕旋转</h2><div class="card" id="set-rotate"></div>' +
      '<h2 style="margin:20px 0 10px;font-size:15px;">系统设置</h2><div class="card" style="padding:0;" id="set-links"></div>';

    var volumeData = {};
    var brightnessVal = 128;
    var autoBrightness = false;
    var autoRotate = false;

    var volumeStreams = [
      { key: 'music', label: '媒体', max: 15 },
      { key: 'ring', label: '铃声', max: 7 },
      { key: 'notification', label: '通知', max: 7 },
      { key: 'alarm', label: '闹钟', max: 7 }
    ];

    var settingsLinks = [
      { name: 'WiFi', action: 'android.settings.WIFI_SETTINGS', icon: 'M5 12.55a11 11 0 0 1 14.08 0M1.42 9a16 16 0 0 1 21.16 0M8.53 16.11a6 6 0 0 1 6.95 0M12 20h.01' },
      { name: '蓝牙', action: 'android.settings.BLUETOOTH_SETTINGS', icon: 'M6.5 6.5l11 11M12 2v20M17 7l-5 5-5-5' },
      { name: '显示', action: 'android.settings.DISPLAY_SETTINGS', icon: 'M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z' },
      { name: '声音', action: 'android.settings.SOUND_SETTINGS', icon: 'M9 18V5l12-3v13M9 19c0 1.1-1.3 2-3 2s-3-.9-3-2 1.3-2 3-2 3 .9 3 2z' },
      { name: '应用', action: 'android.settings.APPLICATION_SETTINGS', icon: 'M4 4h16v16H4z' },
      { name: '开发者选项', action: 'android.settings.APPLICATION_DEVELOPMENT_SETTINGS', icon: 'M16 18l6-6-6-6M8 6l-6 6 6 6', note: '需先在系统设置中启用' },
      { name: '电池', action: 'android.settings.BATTERY_SAVER_SETTINGS', icon: 'M13 2L3 14h9l-1 8 10-12h-9l1-8z' },
      { name: '存储', action: 'android.settings.INTERNAL_STORAGE_SETTINGS', icon: 'M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z' },
      { name: '通知', action: 'android.settings.NOTIFICATION_LISTENER_SETTINGS', icon: 'M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 0 1-3.46 0' },
      { name: '位置', action: 'android.settings.LOCATION_SOURCE_SETTINGS', icon: 'M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z' },
      { name: '安全', action: 'android.settings.SECURITY_SETTINGS', icon: 'M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z' },
      { name: '关于', action: 'android.settings.DEVICE_INFO_SETTINGS', icon: 'M12 22c5.5 0 10-4.5 10-10S17.5 2 12 2 2 6.5 2 12s4.5 10 10 10zM12 16v-4M12 8h.01' }
    ];

    // Build settings links
    var linksHtml = '';
    for (var li = 0; li < settingsLinks.length; li++) {
      var lk = settingsLinks[li];
      linksHtml += '<div class="row" style="cursor:pointer;" data-action="' + esc(lk.action) + '"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="' + lk.icon + '"/></svg><div class="row-text"><div class="row-title">' + esc(lk.name) + '</div>' + (lk.note ? '<div style="font-size:11px;color:var(--text-muted);">' + esc(lk.note) + '</div>' : '') + '</div><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg></div>';
    }
    document.getElementById('set-links').innerHTML = linksHtml;

    var linkRows = document.querySelectorAll('#set-links [data-action]');
    for (var lri = 0; lri < linkRows.length; lri++) {
      linkRows[lri].addEventListener('click', function () {
        var action = this.getAttribute('data-action');
        postAction('/api/open-settings?action=' + encodeURIComponent(action)).then(function (r) {
          if (!r.success) toast(r.error || '失败', 'error');
        }).catch(function () { toast('失败', 'error'); });
      });
    }

    function renderVolumes() {
      var html = '';
      for (var vi = 0; vi < volumeStreams.length; vi++) {
        var vs = volumeStreams[vi];
        var cur = 0;
        var max = vs.max;
        var v = volumeData[vs.key];
        if (v && typeof v === 'object') { cur = v.current || 0; max = v.max || max; }
        else if (v !== undefined) { cur = v; }
        html += '<div class="row" style="flex-wrap:wrap;gap:8px;"><div style="width:50px;font-size:12px;color:var(--text-muted);">' + vs.label + '</div><input type="range" min="0" max="' + max + '" value="' + cur + '" data-stream="' + vs.key + '" style="flex:1;min-width:0;" /><div style="width:24px;font-size:12px;text-align:right;color:var(--text-muted);" id="vol-val-' + vs.key + '">' + cur + '</div></div>';
      }
      document.getElementById('set-volumes').innerHTML = html;

      var volSliders = document.querySelectorAll('#set-volumes [data-stream]');
      for (var vsi = 0; vsi < volSliders.length; vsi++) {
        volSliders[vsi].addEventListener('input', function () {
          var stream = this.getAttribute('data-stream');
          var val = parseInt(this.value);
          document.getElementById('vol-val-' + stream).textContent = val;
          if (volumeData[stream] && typeof volumeData[stream] === 'object') {
            volumeData[stream].current = val;
          } else {
            volumeData[stream] = val;
          }
          postAction('/api/volume?stream=' + stream + '&value=' + val).catch(function () {});
        });
      }
    }

    function renderBrightness() {
      var html = '<div class="row" style="justify-content:space-between;"><div><div style="font-size:14px;font-weight:600;">自动亮度</div><div style="font-size:12px;color:var(--text-muted);">' + (autoBrightness ? '已开启' : '已关闭') + '</div></div><div class="toggle' + (autoBrightness ? ' active' : '') + '" id="set-auto-brightness"></div></div>' +
        '<div class="row" style="margin-top:8px;' + (autoBrightness ? 'opacity:0.4;pointer-events:none;' : '') + '" id="set-brightness-row"><input type="range" min="0" max="255" value="' + brightnessVal + '" id="set-brightness-slider" style="flex:1;" /><div style="width:32px;font-size:12px;text-align:right;color:var(--text-muted);">' + brightnessVal + '</div></div>' +
        '<div style="font-size:11px;color:var(--text-muted);margin-top:4px;">如无法调节，请在系统设置中授予"修改系统设置"权限</div>';
      document.getElementById('set-brightness').innerHTML = html;

      document.getElementById('set-auto-brightness').addEventListener('click', function () {
        autoBrightness = !autoBrightness;
        postAction('/api/brightness?autoMode=' + autoBrightness).then(function (r) {
          if (r && r.error) {
            toast(r.error, 'error');
            autoBrightness = !autoBrightness;
          }
          renderBrightness();
        }).catch(function () { renderBrightness(); });
      });

      var slider = document.getElementById('set-brightness-slider');
      if (slider) {
        slider.addEventListener('input', function () {
          brightnessVal = parseInt(this.value);
          this.nextElementSibling.textContent = brightnessVal;
          postAction('/api/brightness?value=' + brightnessVal).then(function (r) {
            if (r && r.error) toast(r.error, 'error');
          }).catch(function () {});
        });
      }
    }

    function renderRotate() {
      document.getElementById('set-rotate').innerHTML = '<div class="row" style="justify-content:space-between;"><div><div style="font-size:14px;font-weight:600;">自动旋转</div><div style="font-size:12px;color:var(--text-muted);">' + (autoRotate ? '已开启' : '已关闭') + '</div></div><div class="toggle' + (autoRotate ? ' active' : '') + '" id="set-auto-rotate"></div></div>';

      document.getElementById('set-auto-rotate').addEventListener('click', function () {
        autoRotate = !autoRotate;
        postAction('/api/rotation?enabled=' + autoRotate).catch(function () {});
        renderRotate();
      });
    }

    // Load all data
    Promise.all([
      getJSON('/api/device').catch(function () { return {}; }),
      getJSON('/api/battery').catch(function () { return {}; }),
      getJSON('/api/storage').catch(function () { return {}; }),
      getJSON('/api/wifi-status').catch(function () { return {}; }),
      getJSON('/api/volume').catch(function () { return {}; }),
      getJSON('/api/brightness').catch(function () { return {}; }),
      getJSON('/api/rotation').catch(function () { return {}; })
    ]).then(function (results) {
      var d = results[0], b = results[1], s = results[2], w = results[3];
      volumeData = results[4] || {};
      brightnessVal = (results[5] && results[5].value) || 128;
      autoBrightness = (results[5] && results[5].autoMode) || false;
      autoRotate = (results[6] && results[6].enabled) || false;

      document.getElementById('set-device').textContent = d.model || '—';
      document.getElementById('set-device-info').textContent = (d.manufacturer || '') + ' | Android ' + (d.release || '');

      var batLevel = b.level || 0;
      document.getElementById('set-battery').textContent = batLevel + '%';
      var batBar = document.getElementById('set-battery-bar');
      batBar.style.width = batLevel + '%';
      batBar.style.background = batLevel > 50 ? '#1e8e3e' : (batLevel > 20 ? '#f9ab00' : '#d93025');
      document.getElementById('set-battery-info').textContent = (b.statusText || '') + ' | ' + (b.healthText || '');

      var storUsed = s.used || 0;
      var storTotal = s.total || 1;
      document.getElementById('set-storage').textContent = (s.usedHuman || '—') + ' / ' + (s.totalHuman || '—');
      document.getElementById('set-storage-bar').style.width = Math.round(storUsed / storTotal * 100) + '%';
      document.getElementById('set-storage-info').textContent = (s.availableHuman || '') + ' 剩余';

      document.getElementById('set-wifi').textContent = w.ssid || '未连接';
      document.getElementById('set-wifi-info').textContent = (w.stateText || '') + (w.rssi ? ' ' + w.rssi + ' dBm' : '');

      renderVolumes();
      renderBrightness();
      renderRotate();
    });
  }

  // ---- Server status check ----
  function checkServer() {
    fetch('/api/device').then(function (res) {
      var dot = document.getElementById('status-dot');
      var text = document.getElementById('status-text');
      var parent = dot ? dot.parentElement : null;
      if (res.ok) {
        if (parent) parent.className = 'server-pill server-status online';
        if (text) text.textContent = '已连接';
      } else {
        if (parent) parent.className = 'server-pill server-status offline';
        if (text) text.textContent = '未连接';
      }
    }).catch(function () {
      var dot = document.getElementById('status-dot');
      var text = document.getElementById('status-text');
      var parent = dot ? dot.parentElement : null;
      if (parent) parent.className = 'server-pill server-status offline';
      if (text) text.textContent = '未连接';
    });
  }

  // Router
  window.addEventListener('hashchange', navigate);
  if (!location.hash) location.hash = '#/';
  navigate();

  // Server status
  checkServer();
  setInterval(checkServer, 10000);

})();
