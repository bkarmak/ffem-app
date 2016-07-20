/*
 * Copyright (C) Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Caddisfly
 *
 * Akvo Caddisfly is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 * either version 3 of the License or any later version.
 *
 * Akvo Caddisfly is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.caddisfly.sensor.colorimetry.strip.camera;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.akvo.caddisfly.R;
import org.akvo.caddisfly.sensor.colorimetry.strip.model.StripTest;
import org.akvo.caddisfly.sensor.colorimetry.strip.util.Constant;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Activities that contain this fragment must implement the
 * {@link CameraViewListener} interface
 * to handle interaction events.
 * Use the {@link InstructionFragment#newInstance} factory method to
 * create an instance of this fragment.
 * <p/>
 * This fragment shows instructions for a particular strip test. They are read from strips.json in assets
 */
public class InstructionFragment extends CameraSharedFragmentBase {

    private CameraViewListener mListener;

    public InstructionFragment() {
        // Required empty public constructor
    }

    public static InstructionFragment newInstance(String brandName) {
        InstructionFragment fragment = new InstructionFragment();
        Bundle args = new Bundle();
        args.putString(Constant.BRAND, brandName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_instruction, container, false);
        Button startButton = (Button) rootView.findViewById(R.id.button_start);
        LinearLayout linearLayout = (LinearLayout) rootView.findViewById(R.id.layout_information);

        TextView textTitle = (TextView) rootView.findViewById(R.id.textToolbarTitle);
        if (textTitle != null) {
            textTitle.setText(R.string.instructions);
        }

        if (getArguments() != null) {

            String brandName = getArguments().getString(Constant.BRAND);

            StripTest stripTest = new StripTest();
            JSONArray instructions = stripTest.getBrand(brandName).getInstructions();
            TextView textView;
            try {
                for (int i = 0; i < instructions.length(); i++) {

                    String instr = instructions.getJSONObject(i).getString("text");

                    String[] instrArray = instr.split("<!");

                    for (String anInstrArray : instrArray) {
                        textView = new TextView(getActivity());
                        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                                getResources().getDimension(R.dimen.mediumTextSize));

                        int padBottom = (int) getResources().getDimension(R.dimen.activity_vertical_margin);
                        textView.setPadding(0, 0, 0, padBottom);

                        int indexImp = anInstrArray.indexOf(">");
                        if (indexImp >= 0) {
                            textView.setTextColor(Color.RED);
                        } else {
                            textView.setTextColor(Color.DKGRAY);
                        }
                        String text = anInstrArray.replaceAll(">", "");
                        if (!text.isEmpty()) {
                            textView.append(text);
                            linearLayout.addView(textView);
                        }
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.nextFragment();
            }
        });
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (CameraViewListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement CameraViewListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getView() != null) {
            final FrameLayout parentView = (FrameLayout) getActivity()
                    .findViewById(((View) getView().getParent()).getId());
            ViewGroup.LayoutParams params = parentView.getLayoutParams();
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            parentView.setLayoutParams(params);
        }

    }
}
