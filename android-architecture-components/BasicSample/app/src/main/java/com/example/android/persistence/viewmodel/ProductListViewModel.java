/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.persistence.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;

import com.example.android.persistence.BasicApp;
import com.example.android.persistence.db.entity.ProductEntity;

import java.util.List;

public class ProductListViewModel extends AndroidViewModel {

    // MediatorLiveData can observe other LiveData objects and react on their emissions.
    // mediatorlivedata可以观察其他livedata对象和在他们改变时作出反应。
    private final MediatorLiveData<List<ProductEntity>> mObservableProducts;

    public ProductListViewModel(Application application) {
        super(application);

        mObservableProducts = new MediatorLiveData<>();
        // set by default null, until we get data from the database.
        // 默认设置为null，直到我们从数据库中获取数据为止。
        mObservableProducts.setValue(null);

        LiveData<List<ProductEntity>> products = ((BasicApp) application).getRepository()
                .getProducts();

        // observe the changes of the products from the database and forward them
        // 从数据库中观察产品的变化并转发它们。
        // ::是java 8里引入lambda后的一种用法，表示引用，比如静态方法的引用String::valueOf;比如构造器的引用，ArrayList::new。
        mObservableProducts.addSource(products, mObservableProducts::setValue);
    }

    /**
     * Expose the LiveData Products query so the UI can observe it.
     *
     * 暴露livedata产品查询UI可以观察到它。
     * LiveData 是一个数据持有者类，它持有一个值并允许观察该值。
     * 不同于普通的可观察者，LiveData 遵守应用程序组件的生命周期，
     * 以便 Observer 可以指定一个其应该遵守的 Lifecycle。
     */
    public LiveData<List<ProductEntity>> getProducts() {
        return mObservableProducts;
    }
}
