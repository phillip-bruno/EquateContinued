package com.wolfcola.equatecontinued.view;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wolfcola.equatecontinued.Calculator;
import com.wolfcola.equatecontinued.R;
import com.wolfcola.equatecontinued.Result;
import com.wolfcola.equatecontinued.databinding.FragmentResultListBinding;
import com.wolfcola.equatecontinued.databinding.ListItemResultBinding;

import java.util.List;

public class ResultListFragment extends Fragment {
    private CalcViewModel mViewModel;
    private List<Result> mResultArray;
    private RecyclerView mRecyclerView;
    private ResultAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return FragmentResultListBinding.inflate(inflater, container, false).getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = new ViewModelProvider(requireActivity()).get(CalcViewModel.class);
        mResultArray = mViewModel.getCalc().getResultList();

        mRecyclerView = FragmentResultListBinding.bind(view).resultRecyclerView;
        // Stack from the end so a short history sits against the bottom of the
        // list, directly above the live expression, instead of leaving a gap there.
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);
        mRecyclerView.setLayoutManager(layoutManager);
        mAdapter = new ResultAdapter();
        mRecyclerView.setAdapter(mAdapter);
    }

    /**
     * Update the list and scroll to the bottom
     *
     * @param instantScroll if true, scroll instantly; otherwise use smooth scroll
     */
    public void refresh(boolean instantScroll) {
        if (mAdapter == null) return;
        mAdapter.notifyDataSetChanged();

        int lastPos = mResultArray.size() - 1;
        if (lastPos < 0) return;

        if (instantScroll) {
            mRecyclerView.scrollToPosition(lastPos);
        } else {
            mRecyclerView.smoothScrollToPosition(lastPos);
        }
    }

    private class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ResultViewHolder> {

        @NonNull
        @Override
        public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ResultViewHolder(ListItemResultBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
            Result result = mResultArray.get(position);
            holder.bind(result);
        }

        @Override
        public int getItemCount() {
            return mResultArray.size();
        }

        class ResultViewHolder extends RecyclerView.ViewHolder {
            final ListItemResultBinding b;

            ResultViewHolder(@NonNull ListItemResultBinding binding) {
                super(binding.getRoot());
                b = binding;
            }

            void bind(Result result) {
                b.listItemResultCurrencyTimestamp.setVisibility(View.GONE);

                if (result != null && result.containsUnits()) {
                    String text = getResources().getString(R.string.word_Converted) +
                            " " + result.getQueryUnitTextLong() +
                            " " + getResources().getString(R.string.word_to) +
                            " " + result.getAnswerUnitTextLong() + ":";
                    b.listItemResultConvertUnitDesc.setText(ViewUtils.fromHtml("<i>" + text + "</i>"));
                    b.listItemResultConvertUnitDesc.setVisibility(View.VISIBLE);

                    String timestamp = result.getTimestamp();
                    if (!timestamp.equals("")) {
                        b.listItemResultCurrencyTimestamp.setText(timestamp);
                        b.listItemResultCurrencyTimestamp.setVisibility(View.VISIBLE);
                    }
                } else {
                    b.listItemResultConvertUnitDesc.setVisibility(View.GONE);
                }

                b.listItemResultTextPrevQuery.setText(result.getTextQuery());
                b.listItemResultTextPrevAnswer.setText(result.getTextAnswer());

                // Click to load result back into expression
                View.OnClickListener clickListener = view -> {
                    int pos = getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return;
                    Result thisResult = mResultArray.get(pos);
                    Calculator calc = mViewModel.getCalc();

                    String textPassBack = "";
                    if (view == b.listItemResultTextPrevQuery)
                        textPassBack = thisResult.getQueryWithoutSep();
                    if (view == b.listItemResultTextPrevAnswer)
                        textPassBack = thisResult.getAnswerWithoutSep();

                    calc.parseKeyPressed(textPassBack);

                    if (!calc.isUnitSelected() && thisResult.containsUnits()) {
                        int unitPosPassBack;
                        if (view == b.listItemResultTextPrevQuery)
                            unitPosPassBack = thisResult.getQueryUnitPos();
                        else
                            unitPosPassBack = thisResult.getAnswerUnitPos();

                        mViewModel.selectUnitAtUnitArrayPos(unitPosPassBack, thisResult.getUnitTypeKey());
                    }

                    mViewModel.requestScreenUpdate(false);
                };

                b.listItemResultTextPrevQuery.setOnClickListener(clickListener);
                b.listItemResultTextPrevAnswer.setOnClickListener(clickListener);

                // Long click to delete result
                View.OnLongClickListener longClickListener = view -> {
                    int pos = getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return false;

                    Toast toast = Toast.makeText(view.getContext(), R.string.result_deleted, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();

                    mResultArray.remove(pos);
                    notifyItemRemoved(pos);
                    mViewModel.requestScreenUpdate(true);
                    //consume the event so the release doesn't also fire a click
                    return true;
                };

                b.listItemResultTextPrevQuery.setOnLongClickListener(longClickListener);
                b.listItemResultTextPrevAnswer.setOnLongClickListener(longClickListener);
            }
        }
    }
}
