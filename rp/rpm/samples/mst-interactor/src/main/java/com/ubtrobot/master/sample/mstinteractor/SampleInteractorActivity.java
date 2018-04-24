package com.ubtrobot.master.sample.mstinteractor;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.ubtrobot.master.Master;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SampleInteractorActivity extends AppCompatActivity {

    private SampleList mSampleList;

    private String[] mSampleDescList;
    private Method[] mSampleMethods;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RecyclerView recyclerView = new RecyclerView(this);
        setContentView(recyclerView);

        mSampleList = new SampleList(this, Master.get().getOrCreateInteractor("test"));
        loadRecyclerViewItemInfos();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new RecyclerView.Adapter<ViewHolder>() {

            @Override
            public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new ViewHolder(LayoutInflater.from(parent.getContext()).
                        inflate(R.layout.list_item_api, parent, false));
            }

            @Override
            public void onBindViewHolder(ViewHolder holder, int position) {
                holder.apiButton().setText(mSampleDescList[position]);
            }

            @Override
            public int getItemCount() {
                return mSampleDescList.length;
            }
        });
    }

    private void loadRecyclerViewItemInfos() {
        mSampleDescList = getResources().getStringArray(R.array.interactor_sample_list);
        String[] apiMethodNames = getResources().getStringArray(R.array.interactor_sample_method_list);
        if (mSampleDescList.length != apiMethodNames.length) {
            throw new IllegalStateException("Unmatched api methods and descriptions." +
                    "Check strings.xml 's api_method_list and api_desc_list field.");
        }

        mSampleMethods = new Method[apiMethodNames.length];
        for (int i = 0; i < apiMethodNames.length; i++) {
            String apiMethodName = apiMethodNames[i];
            try {
                mSampleMethods[i] = SampleList.class.getMethod(apiMethodName);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(
                        "SampleList class has no method named " + apiMethodName, e);
            }
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {

        private final Button mApiButton;

        ViewHolder(View itemView) {
            super(itemView);

            mApiButton = itemView.findViewById(R.id.api);

            mApiButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //noinspection TryWithIdenticalCatches
                    try {
                        mSampleMethods[getAdapterPosition()].invoke(mSampleList);
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    } catch (InvocationTargetException e) {
                        throw new IllegalStateException(e);
                    }
                }
            });
        }

        public Button apiButton() {
            return mApiButton;
        }
    }
}

