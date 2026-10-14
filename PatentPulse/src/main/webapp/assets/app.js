document.querySelectorAll('[data-copy]').forEach(button => button.addEventListener('click', async () => {
 try { await navigator.clipboard.writeText(document.getElementById(button.dataset.copy).textContent.trim()); button.textContent = 'Copied'; }
 catch { button.textContent = 'Select text to copy'; }
}));
document.getElementById('print-button')?.addEventListener('click', () => window.print());
document.getElementById('research-form')?.addEventListener('submit', event => {
 const file = document.getElementById('document');
 if (event.submitter?.dataset.action === 'submit' && !file.files.length) {
  event.preventDefault(); file.setCustomValidity('Choose a file to record this version.'); file.reportValidity();
 }
 if (file.files[0]?.size > 10 * 1024 * 1024) { event.preventDefault(); file.setCustomValidity('Choose a file no larger than 10 MB.'); file.reportValidity(); }
});
document.getElementById('document')?.addEventListener('change', event => event.target.setCustomValidity(''));
