package com.ideveloper.thermalsniffer;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class CustomSetupDialog extends DialogFragment {

    CustomSetupDialogListener mListener;

    EditText maxTime;
    EditText maxTemp;
    EditText minWind;
    EditText windCal;
    Switch isFahrenheit;
    Switch inMinutes;
    Switch autoScale;

    int mMaxTimeSensitivity;
    int mMaxTempSensitivity;
    int mMinWindSensitivity;
    int mWindCalibration;
    boolean mShowInFahrenheit;
    boolean mShowInMinutes;
    boolean mAutoAdjustTemperatureRange;

    public static CustomSetupDialog newInstance(int maxTimeSensitivity, int maxTempSensitivity, int maxWindSensitivity, int windCalibration, boolean showInFahrenheit, boolean showInMinutes, boolean autoAdjustTemperatureRange) {
        CustomSetupDialog frag = new CustomSetupDialog();
        Bundle args = new Bundle();
        args.putInt("maxTimeSensitivity", maxTimeSensitivity);
        args.putInt("maxTempSensitivity", maxTempSensitivity);
        args.putInt("minWindSensitivity", maxWindSensitivity);
        args.putInt("windCalibration", windCalibration);
        args.putBoolean("showInFahrenheit", showInFahrenheit);
        args.putBoolean("showInMinutes", showInMinutes);
        args.putBoolean("autoAdjustTemperatureRange", autoAdjustTemperatureRange);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        assert getArguments() != null;
        mMaxTimeSensitivity = getArguments().getInt("maxTimeSensitivity");
        mMaxTempSensitivity = getArguments().getInt("maxTempSensitivity");
        mMinWindSensitivity = getArguments().getInt("minWindSensitivity");
        mWindCalibration = getArguments().getInt("windCalibration");
        mShowInFahrenheit = getArguments().getBoolean("showInFahrenheit");
        mShowInMinutes = getArguments().getBoolean("showInMinutes");
        mAutoAdjustTemperatureRange = getArguments().getBoolean("autoAdjustTemperatureRange");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_settings, container);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        maxTime = view.findViewById(R.id.editMaxTime);
        maxTemp = view.findViewById(R.id.editMaxTemp);
        minWind = view.findViewById(R.id.editMinWind);
        windCal = view.findViewById(R.id.editWindDivider);
        isFahrenheit = view.findViewById(R.id.switchFahrenheit);
        inMinutes = view.findViewById(R.id.switchMinutes);
        autoScale = view.findViewById(R.id.switchAutoScale);

        isFahrenheit.setChecked(mShowInFahrenheit);
        inMinutes.setChecked(mShowInMinutes);
        autoScale.setChecked(mAutoAdjustTemperatureRange);

        maxTime.setText(""+mMaxTimeSensitivity);
        maxTemp.setText(""+mMaxTempSensitivity);
        minWind.setText(""+ mMinWindSensitivity);
        windCal.setText(""+mWindCalibration);

        Button dialogButton = view.findViewById(R.id.idButtonOk);
        dialogButton.setOnClickListener(v -> {
            mMaxTimeSensitivity = Integer.parseInt(maxTime.getText().toString());
            if (mMaxTimeSensitivity <= 60) mMaxTimeSensitivity = 60;
            mMaxTempSensitivity = Integer.parseInt(maxTemp.getText().toString());
            if (mMaxTempSensitivity > 20) mMaxTempSensitivity = 20;
            if (mMaxTempSensitivity <= 0) mMaxTempSensitivity = 1;
            mMinWindSensitivity = Integer.parseInt(minWind.getText().toString());
            if (mMinWindSensitivity > 10) mMinWindSensitivity = 10;
            if (mMinWindSensitivity < 0) mMinWindSensitivity = 0;
            mWindCalibration = Integer.parseInt(windCal.getText().toString());
            if (mWindCalibration < 1) mWindCalibration = 1;
            mShowInFahrenheit = isFahrenheit.isChecked();
            mShowInMinutes = inMinutes.isChecked();
            mAutoAdjustTemperatureRange = autoScale.isChecked();
            mListener.onFinishSetupDialog(mMaxTimeSensitivity, mMaxTempSensitivity, mMinWindSensitivity, mWindCalibration, mShowInFahrenheit, mShowInMinutes, mAutoAdjustTemperatureRange);
            dismiss();
        });

    }

    public void setListener(CustomSetupDialogListener listener) {
        mListener = listener;
    }
    public interface CustomSetupDialogListener {
        void onFinishSetupDialog(int maxTimeSensitivity, int maxTempSensitivity, int minWindSensitivity, int windCalibration, boolean showInFahrenheit, boolean showInMinutes, boolean autoAdjustTemperatureRange);
    }

}