const phone = document.getElementById('phone-preview');
const curtain = document.getElementById('curtain-preview');
const openButton = document.getElementById('open-case');
const closeButton = document.getElementById('close-case');
const darkButton = document.getElementById('dark-theme');
const lightButton = document.getElementById('light-theme');

function select(a, b) {
  a.classList.add('is-active');
  a.setAttribute('aria-pressed', 'true');
  b.classList.remove('is-active');
  b.setAttribute('aria-pressed', 'false');
}

openButton.addEventListener('click', () => {
  select(openButton, closeButton);
  curtain.classList.remove('is-hidden');
});
closeButton.addEventListener('click', () => {
  select(closeButton, openButton);
  curtain.classList.add('is-hidden');
});
darkButton.addEventListener('click', () => {
  select(darkButton, lightButton);
  phone.classList.remove('is-light');
});
lightButton.addEventListener('click', () => {
  select(lightButton, darkButton);
  phone.classList.add('is-light');
});
