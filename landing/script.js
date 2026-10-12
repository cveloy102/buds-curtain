const preview = document.getElementById('phone-preview');
const caption = document.getElementById('preview-caption');
const darkButton = document.getElementById('dark-theme');
const lightButton = document.getElementById('light-theme');

// Load both screenshots before the user changes themes.
for (const theme of ['dark', 'light']) {
  const image = new Image();
  image.src = 'assets/stand-' + theme + '.jpg';
}

function showTheme(theme) {
  const isDark = theme === 'dark';
  preview.src = 'assets/stand-' + theme + '.jpg';
  preview.alt = (isDark ? 'Тёмная' : 'Светлая') + ' карточка Xiaomi Buds 5: пример заряда — левый наушник 100%, кейс 64%, правый 100%';
  caption.textContent = (isDark ? 'ТЁМНАЯ' : 'СВЕТЛАЯ') + ' ТЕМА / ПРИМЕР КАРТОЧКИ';
  darkButton.classList.toggle('is-active', isDark);
  lightButton.classList.toggle('is-active', !isDark);
  darkButton.setAttribute('aria-pressed', String(isDark));
  lightButton.setAttribute('aria-pressed', String(!isDark));
}

darkButton.addEventListener('click', () => showTheme('dark'));
lightButton.addEventListener('click', () => showTheme('light'));
