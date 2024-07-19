/*
 * This file is part of mediadeepa.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Copyright (C) Media ex Machina 2024
 * 
 * Inspired by https://codepen.io/RileyB/pen/XQyaXy
 */

window.addEventListener('load', (event) => {
  const body = document.querySelector('body');
  const modalPopup = document.querySelector('.image-modal-popup');

  document.addEventListener('click', () => {
    body.style.overflow = 'auto';
    modalPopup.style.display = 'none';
  });

  document.querySelectorAll('figure.image img').forEach(img => {
    img.addEventListener('click', e => {
      body.style.overflow = 'hidden';
      e.stopPropagation();
      modalPopup.style.display = 'block';
      document.querySelector(`.image-modal-popup img`).src = img.src;
    });
  });
});
