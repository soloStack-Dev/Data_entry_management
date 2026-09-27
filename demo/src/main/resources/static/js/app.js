/* ==========================================================================
   Data Entry Platform - project behaviour
   Loaded with `defer`, so the DOM is already parsed and Bootstrap and HTMX are
   available on window.

   Everything here is small and single-purpose:
     1. toast notifications           (spec 02 section 5)
     2. delete confirmation dialog    (spec 01 section 8)
     3. auto-dismissing form alerts
     4. the short fade on swapped-in table content
   HTMX itself needs no configuration: the templates carry the hx-* attributes.
   ========================================================================== */

(function () {
	'use strict';

	/* --- 1. Toasts ---------------------------------------------------------
	   A lightweight notification area appended to <body>. Used for events that
	   have no form on screen to host a message - most importantly a failed
	   delete, where the table must stay exactly as it is. */
	function toastHost() {
		var host = document.getElementById('app-toast-host');
		if (!host) {
			host = document.createElement('div');
			host.id = 'app-toast-host';
			// Fixed to the top-right on desktop, full width on mobile, above the sticky navbar.
			host.style.cssText = 'position:fixed;top:70px;right:16px;z-index:1090;max-width:360px';
			document.body.appendChild(host);
		}
		return host;
	}

	function showToast(message, variant) {
		if (!message) {
			return;
		}
		var isError = variant === 'danger';
		var el = document.createElement('div');
		el.className = 'alert app-alert alert-' + (isError ? 'danger' : 'success');
		el.setAttribute('role', isError ? 'alert' : 'status');
		el.style.marginBottom = '8px';

		var icon = document.createElement('i');
		icon.className = 'bi me-2 ' + (isError ? 'bi-exclamation-triangle-fill' : 'bi-check-circle-fill');
		icon.setAttribute('aria-hidden', 'true');

		var text = document.createElement('span');
		text.textContent = message;

		var close = document.createElement('button');
		close.type = 'button';
		close.className = 'btn-close ms-auto';
		close.setAttribute('aria-label', 'Close');
		close.addEventListener('click', function () {
			dismiss(el);
		});

		el.appendChild(icon);
		el.appendChild(text);
		el.appendChild(close);
		toastHost().appendChild(el);

		// Notifications stay visible briefly, then fade out (spec 02 section 5).
		window.setTimeout(function () {
			dismiss(el);
		}, isError ? 6000 : 4000);
	}

	/** Fades an element out, then removes it once the CSS transition has run. */
	function dismiss(el) {
		if (!el || !el.parentNode) {
			return;
		}
		el.classList.add('app-alert--closing');
		window.setTimeout(function () {
			if (el.parentNode) {
				el.parentNode.removeChild(el);
			}
		}, 250);
	}

	/* --- 2. Delete confirmation -------------------------------------------
	   The row buttons in fragments/data-table.html only open #deleteModal. This
	   code remembers which row was chosen and, when the user confirms, issues the
	   DELETE through the same endpoint the server exposes. The endpoint answers
	   with a freshly rendered table, so #data-table-wrap is simply replaced. */
	function initDeleteConfirmation() {
		var modalEl = document.getElementById('deleteModal');
		if (!modalEl || typeof bootstrap === 'undefined') {
			return;
		}

		var nameEl = document.getElementById('deleteModalEntry');
		var confirmButton = document.getElementById('deleteConfirmButton');
		var pendingId = null;

		// Fires while the dialog is opening; relatedTarget is the button that triggered it, which
		// is where the row's id and product name were read from.
		modalEl.addEventListener('show.bs.modal', function (event) {
			var trigger = event.relatedTarget;
			if (!trigger) {
				return;
			}
			pendingId = trigger.getAttribute('data-entry-id');
			if (nameEl) {
				nameEl.textContent = trigger.getAttribute('data-entry-name') || 'this entry';
			}
			if (confirmButton) {
				confirmButton.dataset.entryId = pendingId;
			}
		});

		if (!confirmButton) {
			return;
		}

		confirmButton.addEventListener('click', function () {
			if (!pendingId) {
				return;
			}
			// Carry the current search term and page so the refreshed table keeps the same view.
			var searchField = document.querySelector('input[name="search"]');
			var keyword = searchField ? searchField.value.trim() : '';
			var url = '/collection/' + encodeURIComponent(pendingId)
				+ '?search=' + encodeURIComponent(keyword)
				// page=0 because a deletion can leave the current page number out of range.
				+ '&page=0';

			// htmx.ajax is used instead of an hx-delete attribute because the id is only known once
			// the dialog has been opened.
			htmx.ajax('DELETE', url, {
				target: '#data-table-wrap',
				swap: 'innerHTML'
			});

			// Close the dialog immediately; the table updates when the response arrives.
			bootstrap.Modal.getOrCreateInstance(modalEl).hide();
		});
	}

	/* --- 3. Auto-dismissing alerts -----------------------------------------
	   Any element with data-auto-dismiss="<milliseconds>" fades out on its own.
	   The attribute is on the server-rendered alerts, so this works for a message
	   produced by a fresh page load as well as one swapped in by HTMX. */
	function initAutoDismiss(root) {
		var scope = root || document;
		var alerts = scope.querySelectorAll('[data-auto-dismiss]');
		Array.prototype.forEach.call(alerts, function (alertEl) {
			// The element is new after every HTMX swap, so guard against scheduling twice.
			if (alertEl.dataset.dismissScheduled === 'true') {
				return;
			}
			alertEl.dataset.dismissScheduled = 'true';
			window.setTimeout(function () {
				dismiss(alertEl);
			}, parseInt(alertEl.getAttribute('data-auto-dismiss'), 10) || 4000);
		});
	}

	/* --- 4. Swapped-in content animation ----------------------------------
	   Adds a short fade to table content that HTMX has just replaced
	   (spec 02 section 5: "when table content changes, a short fade"). */
	function flashSwap(target) {
		if (!target) {
			return;
		}
		var card = target.querySelector('.app-card') || target;
		// Restart the CSS animation by removing the class, forcing a reflow, then re-adding it.
		card.classList.remove('app-swap-fade');
		void card.offsetWidth;
		card.classList.add('app-swap-fade');
	}

	/* --- HTMX event wiring ------------------------------------------------- */
	document.addEventListener('DOMContentLoaded', function () {
		initDeleteConfirmation();
		initAutoDismiss(document);
	});

	// Runs after every swap, so alerts inside new content are scheduled too.
	document.body.addEventListener('htmx:afterSwap', function (event) {
		initAutoDismiss(event.target);
		flashSwap(event.target);
	});

	// The server broadcasts {"entryDeleted":{"message":"..."}} through the HX-Trigger header.
	// document-level listener, because the event is dispatched on the element that made the request.
	document.body.addEventListener('entryDeleted', function (event) {
		showToast(event.detail && event.detail.message, 'success');
	});

	/* Failed HTMX requests.
	   HTMX does not swap the body of a 4xx/5xx response, which is exactly what protects the table
	   and the form from being replaced by an error page. The reason still needs to reach the user,
	   so the friendly message that GlobalExceptionHandler put in X-App-Error is shown as a toast.

	   A missing header does not mean the server was unreachable: it means the response arrived but
	   nothing handled it, which is a different fault and is reported with the status code. Claiming
	   a connectivity problem here sent debugging in the wrong direction once already. */
	document.body.addEventListener('htmx:responseError', function (event) {
		var xhr = event.detail && event.detail.xhr;
		if (!xhr) {
			return;
		}
		var header = xhr.getResponseHeader('X-App-Error');
		if (header) {
			showToast(header, 'danger');
			return;
		}
		showToast('The server rejected the request (HTTP ' + xhr.status + ').', 'danger');
	});

	/* Genuine connectivity failures. The request got no response at all - the browser refused the
	   connection or dropped it - so this is the only case where "could not be reached" is accurate.
	   Without this handler such a failure is silent, because htmx:responseError never fires. */
	document.body.addEventListener('htmx:sendError', function () {
		showToast('Could not reach the server. Check that the application is still running, then try again.', 'danger');
	});

	/* Safety net: if a dialog is open while its markup is replaced, Bootstrap's backdrop would be
	   left behind and the page would stay greyed out. The dialog lives outside the swap target, so
	   this should not happen - but removing an orphaned backdrop costs nothing. */
	document.body.addEventListener('htmx:beforeSwap', function () {
		var open = document.querySelector('.modal.show');
		if (open && typeof bootstrap !== 'undefined') {
			bootstrap.Modal.getOrCreateInstance(open).hide();
		}
		document.querySelectorAll('.modal-backdrop').forEach(function (node) {
			node.remove();
		});
		document.body.classList.remove('modal-open');
	});

})();
