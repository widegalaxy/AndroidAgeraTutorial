package com.github.captain_miao.agera.tutorial.recycleview;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.widget.Toast;

import com.github.captain_miao.agera.tutorial.R;
import com.github.captain_miao.agera.tutorial.base.BaseActivity;
import com.github.captain_miao.agera.tutorial.helper.PicassoOnScrollListener;
import com.github.captain_miao.agera.tutorial.listener.SimpleObservable;
import com.github.captain_miao.agera.tutorial.model.ApiResult;
import com.github.captain_miao.agera.tutorial.model.GirlInfo;
import com.github.captain_miao.agera.tutorial.supplier.GirlsSupplier;
import com.github.captain_miao.recyclerviewutils.WrapperRecyclerView;
import com.github.captain_miao.recyclerviewutils.common.DefaultLoadMoreFooterView;
import com.github.captain_miao.recyclerviewutils.listener.RefreshRecyclerViewListener;
import com.google.android.agera.Functions;
import com.google.android.agera.Repositories;
import com.google.android.agera.Repository;
import com.google.android.agera.RepositoryConfig;
import com.google.android.agera.Result;
import com.google.android.agera.Supplier;
import com.google.android.agera.Updatable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecycleViewActivity extends BaseActivity implements RefreshRecyclerViewListener, Updatable {
    private static final String TAG = "RecycleViewActivity";

    private WrapperRecyclerView mRefreshRecyclerView;
    private GirlListAdapter mAdapter;
    @Override
    public void init(Bundle savedInstanceState) {
        setContentView(R.layout.activity_recycle_view);
        mRefreshRecyclerView = (WrapperRecyclerView) findViewById(R.id.refresh_recycler_view);
        mAdapter = new GirlListAdapter();
        mRefreshRecyclerView.setAdapter(mAdapter);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRefreshRecyclerView.setLayoutManager(linearLayoutManager);
        mRefreshRecyclerView.setRecyclerViewListener(this);
        mAdapter.setLoadMoreFooterView(new DefaultLoadMoreFooterView(this));
        mRefreshRecyclerView.setPadding(0, 0, 0, 20);

        mRefreshRecyclerView.getRecyclerView().addOnScrollListener(new PicassoOnScrollListener(this));

        setUpRepository();
    }


    @Override
    public void onRefresh() {
        mPagination = 1;
        mObservable.update();
    }

    @Override
    public void onLoadMore(int pagination, int pageSize) {
        mRefreshRecyclerView.showLoadMoreView();
        mPagination = pagination;
        mObservable.update();
    }


    //for agera
    private ExecutorService networkExecutor;
    private SimpleObservable mObservable;
    private Repository<Result<ApiResult<GirlInfo>>> mRepository;

    @Override
     protected void onResume() {
         super.onResume();
         mRepository.addUpdatable(this);
     }

     @Override
     protected void onPause() {
         super.onPause();
         mRepository.removeUpdatable(this);
     }

     @Override
     protected void onDestroy() {
         super.onDestroy();
         networkExecutor.shutdown();
     }

    private int mPagination = 1;
     private void setUpRepository() {
         networkExecutor = Executors.newSingleThreadExecutor();
         mObservable = new SimpleObservable() { };


         mRepository = Repositories.repositoryWithInitialValue(Result.<ApiResult<GirlInfo>>absent())
                 .observe(mObservable)
                 .onUpdatesPerLoop()
                 .goTo(networkExecutor)
                 .getFrom(new GirlsSupplier(new Supplier<Integer>() {
                     @NonNull
                     @Override
                     public Integer get() {
                         return mPagination;
                     }
                 }))

                 .thenTransform(Functions.<Result<ApiResult<GirlInfo>>>identityFunction())
                 .onDeactivation(RepositoryConfig.SEND_INTERRUPT)
                 .compile();
     }

    @Override
    public void update() {
        Result<ApiResult<GirlInfo>> result = mRepository.get();
        if (result.succeeded() && !result.get().error) {
            if (mPagination == 1) {
                mAdapter.clear();
                mAdapter.addAll(result.get().results);
                mRefreshRecyclerView.refreshComplete();
            } else {
                if(result.get().results != null && result.get().results.size() > 0) {
                    mAdapter.addAll(result.get().results, false);
                    int size = result.get().results.size();
                    mAdapter.notifyItemRangeInserted(mAdapter.getItemCount() - size, size);
                } else {
                    Toast.makeText(this, "It's no more data.", Toast.LENGTH_LONG).show();
                }
                mRefreshRecyclerView.loadMoreComplete();
            }
        } else {
            Toast.makeText(this, "load data fail", Toast.LENGTH_LONG).show();
        }
        mRefreshRecyclerView.hideFooterView();
    }
}