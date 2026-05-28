package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */

public interface IBlogService extends IService<Blog> {


    Result queryHotBlog(Integer current);


    Result queryBlogById(Long id);


    Result likeBlog(Long id);

    Result queryBlogLikes(Long id);


    /**
     * 新增探店笔记
     * @param blog
     * @return
     */
    Result saveBlog(Blog blog);

    Result queryBlogOfFollow(Long max, Integer offSet);
}
