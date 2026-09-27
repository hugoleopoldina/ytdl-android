package com.youtube.ytdl.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.youtube.ytdl.R;

public class TextInput extends TextInputLayout {
    public TextInputEditText edit_text;

    // Construtor chamado ao instanciar em java
    public TextInput(Context context) {
        super(context);
        init(context, null);
    }

    // Construtor chamado ao defini-lo em um xml
    public TextInput(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        // Criando uma instancia LayoutInflater para inflar o xml
        LayoutInflater inflater = LayoutInflater.from(context);

        // Recuperando uma view que contem os elementos referente ao xml especificado
        View inflatedLayout = inflater.inflate(R.layout.text_input, this);

        // Recuperando as views infladas
        TextInputLayout input_layout = inflatedLayout.findViewById(R.id.input_layout);
        edit_text = inflatedLayout.findViewById(R.id.edit_text);

        if (attrs != null) {
            // Recuperando a lista de atributos passados para o elemento do xml (TextInput)
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.TextInput);

            // Recuperando e atribuido os atributos personalizados definidos ao TextInputLayout pai
            CharSequence hint = typedArray.getText(R.styleable.TextInput_android_hint).toString();
            Drawable icon = typedArray.getDrawable(R.styleable.TextInput_android_icon);
            int inputType = typedArray.getInt(R.styleable.TextInput_android_inputType,
                    InputType.TYPE_NULL);

            if (hint != null) {
                input_layout.setHint(hint);
            }

            if (icon != null) {
                input_layout.setStartIconDrawable(icon);
            }

            if (inputType != InputType.TYPE_NULL) {
                edit_text.setInputType(inputType);
            }

            // Liberando recursos
            typedArray.recycle();
        }
    }
}
