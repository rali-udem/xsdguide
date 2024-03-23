/**
 * (c) Recherche appliquee en linguistique informatique
 * Fabrizio Gotti
 */
var simpleView = false;

function toggleView() {
    toggleVisibility("optional", simpleView);
    
    var toggler = document.getElementById('toggler');
    toggler.innerHTML = simpleView ? 'Switch to simple view' :  'Switch to advanced view';
    
    simpleView = !simpleView;
}

function toggleVisibility(matchClass, showElement) {
    var elems = document.getElementsByTagName('*'), i;
    for (i in elems) {
        if((' ' + elems[i].className + ' ').indexOf(' ' + matchClass + ' ') > -1) {
            elems[i].style.display = showElement ? "block" : "none";
        }
    }
}