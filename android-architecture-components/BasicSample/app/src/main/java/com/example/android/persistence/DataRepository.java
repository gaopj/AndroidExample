package com.example.android.persistence;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;

import com.example.android.persistence.db.AppDatabase;
import com.example.android.persistence.db.entity.CommentEntity;
import com.example.android.persistence.db.entity.ProductEntity;

import java.util.List;

/**
 * Repository handling the work with products and comments.
 *
 * 处理产品和注释的存储库。单例模式
 */
public class DataRepository {

    private static DataRepository sInstance;

    private final AppDatabase mDatabase;

    // 该类专门用于正确监听别的LiveData实例，并处理它们发出的事件。
    // MediatorLiveData负责将其活动/非活动的状态正确传递到源LiveData。
    private MediatorLiveData<List<ProductEntity>> mObservableProducts;

    private DataRepository(final AppDatabase database) {
        mDatabase = database;
        mObservableProducts = new MediatorLiveData<>();

        // 第一个参数作为被监听的参数
        // 第二个参数是个监听器，当第一个参数变化时调用函数
        mObservableProducts.addSource(mDatabase.productDao().loadAllProducts(),
                productEntities -> {
                    if (mDatabase.getDatabaseCreated().getValue() != null) {
                        // 将任务发布到主线程以设置给定值。
                        // 如果在主线程执行已发布任务之前多次调用此方法，则只最后一个值将被发送。
                        mObservableProducts.postValue(productEntities);
                    }
                });
    }

    // 懒汉式线程安全单例
    public static DataRepository getInstance(final AppDatabase database) {
        if (sInstance == null) {
            synchronized (DataRepository.class) {
                if (sInstance == null) {
                    sInstance = new DataRepository(database);
                }
            }
        }
        return sInstance;
    }

    /**
     * Get the list of products from the database and get notified when the data changes.
     * 从数据库中获取产品列表，并在数据更改时得到通知。
     */
    public LiveData<List<ProductEntity>> getProducts() {
        return mObservableProducts;
    }

    public LiveData<ProductEntity> loadProduct(final int productId) {
        return mDatabase.productDao().loadProduct(productId);
    }

    public LiveData<List<CommentEntity>> loadComments(final int productId) {
        return mDatabase.commentDao().loadComments(productId);
    }
}
