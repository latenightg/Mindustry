package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.*;
import io.anuke.arc.input.*;
import io.anuke.mindustry.core.GameState.*;

import static io.anuke.mindustry.Vars.*;

public class PausedDialog extends FloatingDialog{
    private SaveDialog save = new SaveDialog();
    private LoadDialog load = new LoadDialog();
    private boolean wasClient = false;

    public PausedDialog(){
        super("$menu");
        shouldPause = true;

        shown(this::rebuild);

        keyDown(key -> {
            if(key == KeyCode.ESCAPE || key == KeyCode.BACK){
                hide();
            }
        });
    }

    void rebuild(){
        cont.clear();

        update(() -> {
            if(state.is(State.menu) && isShown()){
                hide();
            }
        });

        if(!mobile){
            float dw = 210f;
            cont.defaults().width(dw).height(50).pad(5f);

            cont.addButton("$back", this::hide).colspan(2).width(dw * 2 + 20f);

            cont.row();
            if(world.isZone()){
                cont.addButton("$techtree", ui.tech::show);
            }else{
                cont.addButton("$database", ui.database::show);
            }
            cont.addButton("$settings", ui.settings::show);

            if(!state.rules.tutorial){
                if(!world.isZone() && !state.isEditor()){
                    cont.row();
                    cont.addButton("$savegame", save::show);
                    cont.addButton("$loadgame", load::show).disabled(b -> net.active());
                }

                cont.row();

                cont.addButton("$hostserver", () -> {
                    if(steam){
                        ui.host.runHost();
                    }else{
                        ui.host.show();
                    }
                }).disabled(b -> net.active()).colspan(2).width(dw * 2 + 20f);
            }

            cont.row();

            cont.addButton("$quit", this::showQuitConfirm).colspan(2).width(dw + 10f);

        }else{
            cont.defaults().size(120f).pad(5);
            float isize = iconsize;

            cont.addRowImageTextButton("$back", "icon-play-2", isize, this::hide);
            cont.addRowImageTextButton("$settings", "icon-tools", isize, ui.settings::show);

            if(!world.isZone() && !state.isEditor()){
                cont.addRowImageTextButton("$save", "icon-save", isize, save::show);

                cont.row();

                cont.addRowImageTextButton("$load", "icon-load", isize, load::show).disabled(b -> net.active());
            }else{
                cont.row();
            }

            cont.addRowImageTextButton("$hostserver.mobile", "icon-host", isize, ui.host::show).disabled(b -> net.active());

            cont.addRowImageTextButton("$quit", "icon-quit", isize, this::showQuitConfirm);
        }
    }

    void showQuitConfirm(){
        ui.showConfirm("$confirm", state.rules.tutorial ? "$quit.confirm.tutorial" : "$quit.confirm", () -> {
            if(state.rules.tutorial){
                Core.settings.put("playedtutorial", true);
                Core.settings.save();
            }
            wasClient = net.client();
            if(net.client()) netClient.disconnectQuietly();
            runExitSave();
            hide();
        });
    }

    public void runExitSave(){
        if(state.isEditor() && !wasClient){
            ui.editor.resumeEditing();
            return;
        }

        if(control.saves.getCurrent() == null || !control.saves.getCurrent().isAutosave() || state.rules.tutorial || wasClient){
            state.set(State.menu);
            logic.reset();
            return;
        }

        ui.loadAnd("$saveload", () -> {
            try{
                control.saves.getCurrent().save();
            }catch(Throwable e){
                e.printStackTrace();
                ui.showException("[accent]" + Core.bundle.get("savefail"), e);
            }
            state.set(State.menu);
            logic.reset();
        });
    }
}
