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

package com.example.android.persistence.db.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

import com.example.android.persistence.model.Comment;

import java.util.Date;

//当一个类用@Entity注解并且被@Database注解中的entities属性所引用，
// Room就会在数据库中为那个entity创建一张表。

// 默认Room会为entity中定义的每一个field都创建一个column。
// 如果一个entity中有你不想持久化的field，那么你可以使用@Ignore来注释它们
@Entity(tableName = "comments",
        foreignKeys = {
        // 建外键
                @ForeignKey(entity = ProductEntity.class,
                        parentColumns = "id",
                        childColumns = "productId",
                        onDelete = ForeignKey.CASCADE)},
        // 建索引
        indices = {@Index(value = "productId")
        })
public class CommentEntity implements Comment {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int productId;
    private String text;
    private Date postedAt;

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    @Override
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public Date getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(Date postedAt) {
        this.postedAt = postedAt;
    }

    public CommentEntity() {
    }

    public CommentEntity(int id, int productId, String text, Date postedAt) {
        this.id = id;
        this.productId = productId;
        this.text = text;
        this.postedAt = postedAt;
    }
}
