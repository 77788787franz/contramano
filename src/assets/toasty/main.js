(function(w) {
    'use strict';

    var toasty = new Toasty({
        transition: "fade",
        duration: 0, // calculated automatically.
        enableSounds: true,
        progressBar: true,
        autoClose: true,
        onShow: function (type) {
            console.log("A toast " + type + " message is shown!");
        },
        onHide: function (type) {
            console.log("The toast " + type + " message is hidden!");
        }
    });

    var btns = document.querySelectorAll('.btn-example');

    var down = document.getElementById('action-download');

    if (down) {
        down.addEventListener('click', function(e) {
            toasty.configure({
                transition: 'slideLeftFade',
                autoClose: true
            }).success("<span class=\"fa fa-download\"></span> Preparing download...", 1000);
        });
    }

    function show(type, message) {
        switch (type) {
            case 'info':
                toasty.info(message);
                break;
            case 'success':
                toasty.success(message);
                break;
            case 'warning':
                toasty.warning(message);
                break;
            case 'error':
                toasty.error(message);
                break;
            default:
                console.error("Error - no toast to show!");
        }
        return type;
    }

    for (var i = 0; i < btns.length; i++) {
        btns[i].addEventListener('click', function(e) {
            e.preventDefault();
            show(this.id, this.title);
        });
    }

    var newTransitionScale = document.getElementById('new-transition-scale');

    if (newTransitionScale) {
        newTransitionScale.addEventListener('click', function(e) {
            e.preventDefault();
            toasty.configure({ classname: "toast" });
            toasty.transition("scale");
            toasty.info("You have been registered a new scale transition correctly.", 2500);
        });
    }

    var alertsReStylized = document.getElementById('alerts-re-stylized');

    if (alertsReStylized) {
        alertsReStylized.addEventListener('click', function(e) {
            e.preventDefault();

            toasty.configure({
                classname: "alert",
                insertBefore: false,
                progressBar: true,
                enableSounds: true
            });

            toasty.transition("scale");

            toasty.info("The toast messages have been re-stylized correctly.", 2500);
        });
    }

})(window);
