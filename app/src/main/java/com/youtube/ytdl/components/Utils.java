package com.youtube.ytdl.components;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.StandardArtwork;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

// Classe de utilidades customizadas
public class Utils {
    public static void setOnFocusInput(Context context, List<ViewGroup> parents, View target) {
        for (View parent : parents) {
            parent.setOnTouchListener((view, event) -> {
                view.performClick();
                target.clearFocus();
                disableVirtualKeyboard(context, target);
                return false;
            });
        }
    }

    // Função responsável por esconder o teclado associado a uma view, a view é necessário para
    // identificar o teclado virtual correspondente à própria view, utilizando um Token
    // View.getWindowToken()
    public static void disableVirtualKeyboard(Context context, View target) {
        InputMethodManager imm = (InputMethodManager)
                context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(target.getWindowToken(), 0);
    }

    // Retorna um objeto Artwork que contém uma capa de album relacionada à url da imagem
    // é feito um conexão para essa url e obtido uma referencia ao fluxos de dados
    // para que seja possível escrever os dados da imagem ao objeto Artwork
    public static Artwork getArtwork(String imageUrl) {
        Artwork artwork = new StandardArtwork();

        try {
            URL url =  new URL(imageUrl);
            InputStream is = url.openStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] imageBytes = new byte[4096];
            int n;

            while ((n = is.read(imageBytes)) > 0) {
                baos.write(imageBytes, 0, n);
            }

            artwork.setBinaryData(baos.toByteArray());
            baos.close();

        } catch (IOException err) {
            err.printStackTrace();
        }

        return artwork;
    }

    // Verifica se as permissões de armazenamento estão permitidas ou negadas. False para negado e
    // True para permitido, caso estiver negado será feito a solicitação para a permissão
    public static boolean checkStoragePermission(Context context, Activity activity) {
        boolean result = false;

        if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT <= 31) {
            int wesPermission = ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

            int resPermission = ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE);

            if (wesPermission == PackageManager.PERMISSION_GRANTED &&
                    resPermission == PackageManager.PERMISSION_GRANTED) {
                result = true;
            } else {
                String[] permissions = { android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(activity, permissions, 0);
            }
        } else {
            result = true;
        }

        return result;
    }
}
