export function toggleGroup(btn, group) {
    if (!btn || !group) return;
    const isHidden = group.style.display === 'none' || group.style.display === '';
    group.style.display = isHidden ? 'block' : 'none';
    btn.classList.toggle("is-active", isHidden);
}

export function updateAdvancedSettingsVisibility(selectedPlatforms) {
    const hasAnyNonTelegram = selectedPlatforms.some(v => !v.startsWith("TELEGRAM"));
    const btnButtons = document.getElementById("btnToggleButtons");
    const btnPoll = document.getElementById("btnTogglePoll");
    const groupButtons = document.getElementById("groupInlineButtons");
    const groupPoll = document.getElementById("groupPoll");
    const shouldDisable = hasAnyNonTelegram;

    if (btnButtons) {
        btnButtons.disabled = shouldDisable;
        btnButtons.style.opacity = shouldDisable ? "0.4" : "1";
        btnButtons.style.pointerEvents = shouldDisable ? "none" : "auto";
        if (shouldDisable && groupButtons) {
            groupButtons.style.display = "none";
            btnButtons.classList.remove("is-active");
        }
    }

    if (btnPoll) {
        btnPoll.disabled = shouldDisable;
        btnPoll.style.opacity = shouldDisable ? "0.4" : "1";
        btnPoll.style.pointerEvents = shouldDisable ? "none" : "auto";
        if (shouldDisable && groupPoll) {
            groupPoll.style.display = "none";
            btnPoll.classList.remove("is-active");
        }
    }
}

export function syncQuiz(saveDraftCallback) {
    const quizCb = document.getElementById('pollIsQuiz') || document.querySelector('input[name="pollIsQuiz"]');
    const multCb = document.getElementById('pollMultipleAnswers') || document.querySelector('input[name="pollMultipleAnswers"]');
    const pollOpts = document.getElementById('pollOptions');
    const radioList = document.getElementById('quizOptionsRadioList');
    const quizGroup = document.getElementById('quizCorrectOptionGroup');
    const hiddenId = document.getElementById('pollCorrectOptionId');

    if (!quizCb || !multCb || !quizGroup || !radioList) return;

    const isQuiz = quizCb.checked;

    if (isQuiz) {
        quizGroup.style.display = 'block';
        const groupLabel = quizGroup.querySelector('label');
        if (groupLabel) {
            groupLabel.style.display = 'block';
            groupLabel.style.textAlign = 'left';
            groupLabel.style.width = '50%';
            groupLabel.style.marginLeft = 'auto';
            groupLabel.style.paddingLeft = '4px';
            groupLabel.style.marginBottom = '12px';
        }
        multCb.disabled = true;
        const parent = multCb.closest('.toggle-field');
        if (parent) {
            parent.style.opacity = '0.5';
            parent.style.pointerEvents = 'none';
        }

        const val = pollOpts ? pollOpts.value : "";
        const lines = val.split('\n').map(l => l.trim()).filter(l => l.length > 0);

        radioList.innerHTML = '';
        let selectedIdx = parseInt(hiddenId ? hiddenId.value : "0") || 0;

        if (lines.length === 0) {
            radioList.innerHTML = '<div style="font-size:0.8rem; color:rgba(255,255,255,0.4); font-style:italic; padding:10px; text-align: right;">Enter options above...</div>';
        } else {
            lines.forEach((line, idx) => {
                const isSelected = (idx === selectedIdx);
                const label = document.createElement('label');
                label.style.cssText = `
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    gap: 15px;
                    padding: 10px 16px;
                    background: ${isSelected ? 'rgba(37, 99, 235, 0.15)' : 'rgba(255, 255, 255, 0.03)'};
                    border-radius: 10px;
                    margin-bottom: 8px;
                    cursor: pointer;
                    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
                    border: 1px solid ${isSelected ? 'rgba(59, 130, 246, 0.5)' : 'rgba(255, 255, 255, 0.05)'};
                    width: 50%;
                    margin-left: auto;
                    box-shadow: ${isSelected ? '0 4px 12px rgba(0, 0, 0, 0.1)' : 'none'};
                `;

                label.onmouseenter = () => {
                    if (!isSelected) label.style.background = 'rgba(255, 255, 255, 0.06)';
                };
                label.onmouseleave = () => {
                    if (!isSelected) label.style.background = 'rgba(255, 255, 255, 0.03)';
                };

                const radio = document.createElement('input');
                radio.type = 'radio';
                radio.name = 'quiz_correct_choice_final';
                radio.value = idx;
                radio.checked = isSelected;
                radio.style.cssText = `
                    margin: 0;
                    width: 18px;
                    height: 18px;
                    cursor: pointer;
                    accent-color: #3b82f6;
                `;

                radio.onchange = () => {
                    if (hiddenId) hiddenId.value = idx;
                    saveDraftCallback();
                    syncQuiz(saveDraftCallback);
                };

                const span = document.createElement('span');
                span.style.cssText = `
                    font-size: 0.9rem;
                    font-weight: 500;
                    color: ${isSelected ? '#60a5fa' : 'var(--dm-text-strong)'};
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                `;
                span.textContent = line;

                label.appendChild(radio);
                label.appendChild(span);
                radioList.appendChild(label);
            });
        }
    } else {
        quizGroup.style.display = 'none';
        multCb.disabled = false;
        const parent = multCb.closest('.toggle-field');
        if (parent) {
            parent.style.opacity = '1';
            parent.style.pointerEvents = 'auto';
        }
    }
}
