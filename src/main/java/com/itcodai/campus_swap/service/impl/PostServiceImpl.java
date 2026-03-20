package com.itcodai.campus_swap.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itcodai.campus_swap.entity.Item;
import com.itcodai.campus_swap.entity.Post;
import com.itcodai.campus_swap.entity.PostFavorite;
import com.itcodai.campus_swap.entity.PostLike;
import com.itcodai.campus_swap.entity.PostTag;
import com.itcodai.campus_swap.entity.Trade;
import com.itcodai.campus_swap.entity.User;
import com.itcodai.campus_swap.mapper.ItemMapper;
import com.itcodai.campus_swap.mapper.PostFavoriteMapper;
import com.itcodai.campus_swap.mapper.PostLikeMapper;
import com.itcodai.campus_swap.mapper.PostMapper;
import com.itcodai.campus_swap.mapper.PostTagMapper;
import com.itcodai.campus_swap.mapper.TagMapper;
import com.itcodai.campus_swap.mapper.TradeMapper;
import com.itcodai.campus_swap.mapper.UserMapper;
import com.itcodai.campus_swap.service.NotificationService;
import com.itcodai.campus_swap.service.PostService;
import com.itcodai.campus_swap.vo.PageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 动态服务实现类
 */
@Slf4j
@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    private final UserMapper userMapper;
    private final PostLikeMapper postLikeMapper;
    private final PostFavoriteMapper postFavoriteMapper;
    private final ItemMapper itemMapper;
    private final TradeMapper tradeMapper;
    private final NotificationService notificationService;
    private final PostTagMapper postTagMapper;
    private final TagMapper tagMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();


    private List<String> parseImages(String images) {
        if (images == null || images.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(images, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /** 查询帖子关联的标签名列表 */
    private List<String> getPostTagNames(Long postId) {
        List<PostTag> postTags = postTagMapper.selectList(
                new LambdaQueryWrapper<PostTag>().eq(PostTag::getPostId, postId));
        if (postTags.isEmpty()) return Collections.emptyList();
        return postTags.stream()
                .map(pt -> tagMapper.selectById(pt.getTagId()))
                .filter(tag -> tag != null)
                .map(tag -> tag.getName())
                .collect(Collectors.toList());
    }

    public PostServiceImpl(UserMapper userMapper,
                           PostLikeMapper postLikeMapper,
                           PostFavoriteMapper postFavoriteMapper,
                           ItemMapper itemMapper,
                           TradeMapper tradeMapper,
                           NotificationService notificationService,
                           PostTagMapper postTagMapper,
                           TagMapper tagMapper) {
        this.userMapper = userMapper;
        this.postLikeMapper = postLikeMapper;
        this.postFavoriteMapper = postFavoriteMapper;
        this.itemMapper = itemMapper;
        this.tradeMapper = tradeMapper;
        this.notificationService = notificationService;
        this.postTagMapper = postTagMapper;
        this.tagMapper = tagMapper;
    }

    @Override
    public PageVO<Map<String, Object>> getPosts(Integer page, Integer size, String sort,
                                                String keyword, String tag, Long currentUserId) {
        Page<Post> postPage = new Page<>(page, size);

        LambdaQueryWrapper<Post> queryWrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.like(Post::getContent, keyword);
        }

        if (tag != null && !tag.trim().isEmpty()) {
            // tag 参数是逗号分隔的标签ID（如 "1,3,5"）
            List<Long> postIds = baseMapper.selectPostIdsByTagIds(tag.trim());
            if (postIds.isEmpty()) {
                // 没有任何动态含这些标签，直接返回空结果
                PageVO<Map<String, Object>> empty = new PageVO<>();
                empty.setRecords(Collections.emptyList());
                empty.setTotal(0L);
                empty.setPage(page);
                empty.setSize(size);
                return empty;
            }
            queryWrapper.in(Post::getId, postIds);
        }

        if ("latest".equals(sort)) {
            queryWrapper.orderByDesc(Post::getCreatedAt);
        } else if ("hot".equals(sort)) {
            queryWrapper.orderByDesc(Post::getHotScore);
        } else {
            queryWrapper.orderByDesc(Post::getCreatedAt);
        }

        Page<Post> result = this.page(postPage, queryWrapper);
        List<Post> posts = result.getRecords();

        // 批量查询当前用户的点赞/收藏状态
        Set<Long> likedPostIds = java.util.Collections.emptySet();
        Set<Long> favoritedPostIds = java.util.Collections.emptySet();
        if (currentUserId != null && !posts.isEmpty()) {
            List<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());

            likedPostIds = postLikeMapper.selectList(
                    new LambdaQueryWrapper<PostLike>()
                            .eq(PostLike::getUserId, currentUserId)
                            .in(PostLike::getPostId, postIds)
            ).stream().map(PostLike::getPostId).collect(Collectors.toSet());

            favoritedPostIds = postFavoriteMapper.selectList(
                    new LambdaQueryWrapper<PostFavorite>()
                            .eq(PostFavorite::getUserId, currentUserId)
                            .in(PostFavorite::getPostId, postIds)
            ).stream().map(PostFavorite::getPostId).collect(Collectors.toSet());
        }

        final Set<Long> finalLikedPostIds = likedPostIds;
        final Set<Long> finalFavoritedPostIds = favoritedPostIds;

        List<Map<String, Object>> records = posts.stream().map(post -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", post.getId());
            map.put("userId", post.getUserId());
            map.put("type", post.getType());
            map.put("content", post.getContent());
            map.put("likeCount", post.getLikeCount());
            map.put("commentCount", post.getCommentCount());
            map.put("favoriteCount", post.getFavoriteCount());
            map.put("createdAt", post.getCreatedAt());
            map.put("isLiked", finalLikedPostIds.contains(post.getId()));
            map.put("isFavorited", finalFavoritedPostIds.contains(post.getId()));
            map.put("images", parseImages(post.getImages()));

            User user = userMapper.selectById(post.getUserId());
            if (user != null) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("username", user.getNickname());
                userInfo.put("avatar", user.getAvatar());
                map.put("user", userInfo);
            }

            // 关联物品信息（类型1：发布物品；类型3：分享物品）
            if (post.getItemId() != null) {
                Item item = itemMapper.selectById(post.getItemId());
                if (item != null) {
                    Map<String, Object> itemInfo = new HashMap<>();
                    itemInfo.put("id", item.getId());
                    itemInfo.put("title", item.getTitle());
                    itemInfo.put("price", item.getPrice());
                    itemInfo.put("coverImage", item.getCoverImage());
                    itemInfo.put("description", item.getDescription());
                    itemInfo.put("category", item.getCategory());
                    map.put("item", itemInfo);
                }
            }

            map.put("tags", getPostTagNames(post.getId()));

            return map;
        }).collect(Collectors.toList());

        PageVO<Map<String, Object>> pageVO = new PageVO<>();
        pageVO.setRecords(records);
        pageVO.setTotal(result.getTotal());
        pageVO.setPage(page);
        pageVO.setSize(size);

        return pageVO;
    }

    @Override
    public Map<String, Object> getPostDetail(Long postId, Long currentUserId) {
        Post post = this.getById(postId);
        if (post == null) {
            return null;
        }

        boolean isLiked = false;
        boolean isFavorited = false;
        if (currentUserId != null) {
            isLiked = postLikeMapper.selectCount(
                    new LambdaQueryWrapper<PostLike>()
                            .eq(PostLike::getPostId, postId)
                            .eq(PostLike::getUserId, currentUserId)
            ) > 0;
            isFavorited = postFavoriteMapper.selectCount(
                    new LambdaQueryWrapper<PostFavorite>()
                            .eq(PostFavorite::getPostId, postId)
                            .eq(PostFavorite::getUserId, currentUserId)
            ) > 0;
        }

        Map<String, Object> map = new HashMap<>();
        map.put("id", post.getId());
        map.put("userId", post.getUserId());
        map.put("type", post.getType());
        map.put("content", post.getContent());
        map.put("likeCount", post.getLikeCount());
        map.put("commentCount", post.getCommentCount());
        map.put("favoriteCount", post.getFavoriteCount());
        map.put("createdAt", post.getCreatedAt());
        map.put("isLiked", isLiked);
        map.put("isFavorited", isFavorited);
        map.put("images", parseImages(post.getImages()));

        User user = userMapper.selectById(post.getUserId());
        if (user != null) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getNickname());
            userInfo.put("avatar", user.getAvatar());
            map.put("user", userInfo);
        }

        if (post.getItemId() != null) {
            Item item = itemMapper.selectById(post.getItemId());
            if (item != null) {
                Map<String, Object> itemInfo = new HashMap<>();
                itemInfo.put("id", item.getId());
                itemInfo.put("title", item.getTitle());
                itemInfo.put("price", item.getPrice());
                itemInfo.put("coverImage", item.getCoverImage());
                itemInfo.put("description", item.getDescription());
                itemInfo.put("category", item.getCategory());
                map.put("item", itemInfo);
            }
        }

        map.put("tags", getPostTagNames(post.getId()));
        return map;
    }

    @Override
    @Transactional
    public boolean likePost(Long postId, Long userId) {
        // 检查是否已点赞
        Long count = postLikeMapper.selectCount(
                new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getPostId, postId)
                        .eq(PostLike::getUserId, userId)
        );
        if (count > 0) {
            log.info("用户 {} 已点赞过动态 {}，忽略重复点赞", userId, postId);
            return false;
        }

        PostLike like = new PostLike();
        like.setPostId(postId);
        like.setUserId(userId);
        postLikeMapper.insert(like);

        baseMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("like_count = like_count + 1")
                .setSql("hot_score = like_count * 3 + comment_count * 2 + favorite_count * 4 + share_count * 1 + view_count * 0.1"));

        // 通知帖主
        try {
            Post post = this.getById(postId);
            if (post != null) {
                notificationService.send(post.getUserId(), userId, "LIKE", postId, null);
            }
        } catch (Exception e) {
            log.error("发送点赞通知失败 postId={} userId={}", postId, userId, e);
        }

        log.info("用户 {} 点赞了动态 {}", userId, postId);
        return true;
    }

    @Override
    @Transactional
    public boolean unlikePost(Long postId, Long userId) {
        int deleted = postLikeMapper.delete(
                new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getPostId, postId)
                        .eq(PostLike::getUserId, userId)
        );
        if (deleted == 0) {
            return false;
        }

        baseMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("like_count = GREATEST(like_count - 1, 0)")
                .setSql("hot_score = GREATEST(like_count - 1, 0) * 3 + comment_count * 2 + favorite_count * 4 + share_count * 1 + view_count * 0.1"));

        log.info("用户 {} 取消点赞动态 {}", userId, postId);
        return true;
    }

    @Override
    @Transactional
    public boolean favoritePost(Long postId, Long userId) {
        // 检查是否已收藏
        Long count = postFavoriteMapper.selectCount(
                new LambdaQueryWrapper<PostFavorite>()
                        .eq(PostFavorite::getPostId, postId)
                        .eq(PostFavorite::getUserId, userId)
        );
        if (count > 0) {
            log.info("用户 {} 已收藏过动态 {}，忽略重复收藏", userId, postId);
            return false;
        }

        PostFavorite favorite = new PostFavorite();
        favorite.setPostId(postId);
        favorite.setUserId(userId);
        postFavoriteMapper.insert(favorite);

        baseMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("favorite_count = favorite_count + 1")
                .setSql("hot_score = like_count * 3 + comment_count * 2 + favorite_count * 4 + share_count * 1 + view_count * 0.1"));

        // 通知帖主
        try {
            Post post = this.getById(postId);
            if (post != null) {
                notificationService.send(post.getUserId(), userId, "FAVORITE", postId, null);
            }
        } catch (Exception e) {
            log.error("发送收藏通知失败 postId={} userId={}", postId, userId, e);
        }

        log.info("用户 {} 收藏了动态 {}", userId, postId);
        return true;
    }

    @Override
    @Transactional
    public boolean unfavoritePost(Long postId, Long userId) {
        int deleted = postFavoriteMapper.delete(
                new LambdaQueryWrapper<PostFavorite>()
                        .eq(PostFavorite::getPostId, postId)
                        .eq(PostFavorite::getUserId, userId)
        );
        if (deleted == 0) {
            return false;
        }

        baseMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("favorite_count = GREATEST(favorite_count - 1, 0)")
                .setSql("hot_score = like_count * 3 + comment_count * 2 + GREATEST(favorite_count - 1, 0) * 4 + share_count * 1 + view_count * 0.1"));

        log.info("用户 {} 取消收藏动态 {}", userId, postId);
        return true;
    }

    @Override
    @Transactional
    public Long createPost(Post post, Long userId) {
        post.setUserId(userId);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setFavoriteCount(0);
        post.setViewCount(0);
        post.setShareCount(0);
        post.setHotScore(0.0);
        this.save(post);

        // 保存标签关联
        List<Long> tagIds = post.getTagIds();
        if (tagIds != null && !tagIds.isEmpty()) {
            for (Long tagId : tagIds) {
                PostTag pt = new PostTag();
                pt.setPostId(post.getId());
                pt.setTagId(tagId);
                postTagMapper.insert(pt);
            }
        }

        log.info("用户 {} 创建了动态 {}", userId, post.getId());
        return post.getId();
    }

    @Override
    public void incrementView(Long postId) {
        baseMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("view_count = view_count + 1")
                .setSql("hot_score = like_count * 3 + comment_count * 2 + favorite_count * 4 + share_count * 1 + (view_count + 1) * 0.1"));
    }

    @Override
    public boolean sharePost(Long postId) {
        int rows = baseMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("share_count = share_count + 1")
                .setSql("hot_score = like_count * 3 + comment_count * 2 + favorite_count * 4 + (share_count + 1) * 1 + view_count * 0.1"));
        return rows > 0;
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            LambdaQueryWrapper<Post> todayQuery = new LambdaQueryWrapper<>();
            todayQuery.ge(Post::getCreatedAt, LocalDate.now().atStartOfDay());
            long todayPosts = this.count(todayQuery);
            long totalPosts = this.count();
            long totalUsers = userMapper.selectCount(null);

            long todaySwaps = tradeMapper.selectCount(
                    new LambdaQueryWrapper<Trade>()
                            .eq(Trade::getStatus, "COMPLETED")
                            .ge(Trade::getCompletedAt, LocalDate.now().atStartOfDay())
            );

            stats.put("todayPosts", todayPosts);
            stats.put("todaySwaps", todaySwaps);
            stats.put("totalPosts", totalPosts);
            stats.put("totalUsers", totalUsers);
        } catch (Exception e) {
            log.error("获取统计数据失败", e);
            stats.put("todayPosts", 0);
            stats.put("todaySwaps", 0);
            stats.put("totalPosts", 0);
            stats.put("totalUsers", 0);
        }
        return stats;
    }

    @Override
    @Transactional
    public boolean updatePost(Long postId, Post updateData, Long userId) {
        Post post = this.getById(postId);
        if (post == null) {
            return false;
        }
        if (!post.getUserId().equals(userId)) {
            log.warn("用户 {} 无权编辑动态 {}", userId, postId);
            return false;
        }

        boolean hasChange = false;
        if (updateData.getContent() != null && !updateData.getContent().trim().isEmpty()) {
            post.setContent(updateData.getContent().trim());
            hasChange = true;
        }
        if (updateData.getImages() != null) {
            post.setImages(updateData.getImages());
            hasChange = true;
        }
        if (updateData.getItemId() != null) {
            post.setItemId(updateData.getItemId());
            hasChange = true;
        }

        if (!hasChange) {
            return true;
        }

        boolean success = this.updateById(post);
        if (success) {
            log.info("用户 {} 编辑了动态 {}", userId, postId);
        }
        return success;
    }

    @Override
    @Transactional
    public boolean deletePost(Long postId, Long userId) {
        Post post = this.getById(postId);
        if (post == null) {
            return false;
        }
        if (!post.getUserId().equals(userId)) {
            log.warn("用户 {} 无权删除动态 {}", userId, postId);
            return false;
        }
        boolean success = this.removeById(postId);
        if (success) {
            log.info("用户 {} 删除了动态 {}", userId, postId);
        }
        return success;
    }

    @Override
    public PageVO<Map<String, Object>> getUserPosts(Long userId, int page, int size, Long currentUserId) {
        Page<Post> postPage = new Page<>(page, size);
        LambdaQueryWrapper<Post> queryWrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, userId)
                .orderByDesc(Post::getCreatedAt);
        Page<Post> result = this.page(postPage, queryWrapper);
        List<Post> posts = result.getRecords();

        Set<Long> likedPostIds = java.util.Collections.emptySet();
        Set<Long> favoritedPostIds = java.util.Collections.emptySet();
        if (currentUserId != null && !posts.isEmpty()) {
            List<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
            likedPostIds = postLikeMapper.selectList(
                    new LambdaQueryWrapper<PostLike>()
                            .eq(PostLike::getUserId, currentUserId)
                            .in(PostLike::getPostId, postIds)
            ).stream().map(PostLike::getPostId).collect(Collectors.toSet());
            favoritedPostIds = postFavoriteMapper.selectList(
                    new LambdaQueryWrapper<PostFavorite>()
                            .eq(PostFavorite::getUserId, currentUserId)
                            .in(PostFavorite::getPostId, postIds)
            ).stream().map(PostFavorite::getPostId).collect(Collectors.toSet());
        }

        final Set<Long> finalLikedPostIds = likedPostIds;
        final Set<Long> finalFavoritedPostIds = favoritedPostIds;

        User author = userMapper.selectById(userId);
        Map<String, Object> userInfo = new HashMap<>();
        if (author != null) {
            userInfo.put("id", author.getId());
            userInfo.put("username", author.getNickname());
            userInfo.put("avatar", author.getAvatar());
        }

        List<Map<String, Object>> records = posts.stream().map(post -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", post.getId());
            map.put("userId", post.getUserId());
            map.put("type", post.getType());
            map.put("content", post.getContent());
            map.put("likeCount", post.getLikeCount());
            map.put("commentCount", post.getCommentCount());
            map.put("favoriteCount", post.getFavoriteCount());
            map.put("createdAt", post.getCreatedAt());
            map.put("isLiked", finalLikedPostIds.contains(post.getId()));
            map.put("isFavorited", finalFavoritedPostIds.contains(post.getId()));
            map.put("images", parseImages(post.getImages()));
            map.put("user", userInfo);
            if (post.getItemId() != null) {
                Item item = itemMapper.selectById(post.getItemId());
                if (item != null) {
                    Map<String, Object> itemInfo = new HashMap<>();
                    itemInfo.put("id", item.getId());
                    itemInfo.put("title", item.getTitle());
                    itemInfo.put("price", item.getPrice());
                    itemInfo.put("coverImage", item.getCoverImage());
                    itemInfo.put("description", item.getDescription());
                    itemInfo.put("category", item.getCategory());
                    map.put("item", itemInfo);
                }
            }
            map.put("tags", getPostTagNames(post.getId()));
            return map;
        }).collect(Collectors.toList());

        PageVO<Map<String, Object>> pageVO = new PageVO<>();
        pageVO.setRecords(records);
        pageVO.setTotal(result.getTotal());
        pageVO.setPage(page);
        pageVO.setSize(size);
        return pageVO;
    }

    @Override
    public PageVO<Map<String, Object>> getFavoritePosts(Long userId, int page, int size) {
        // 分页查询该用户的收藏记录，按收藏时间倒序
        Page<PostFavorite> favPage = new Page<>(page, size);
        Page<PostFavorite> favResult = postFavoriteMapper.selectPage(favPage,
                new LambdaQueryWrapper<PostFavorite>()
                        .eq(PostFavorite::getUserId, userId)
                        .orderByDesc(PostFavorite::getCreatedAt));

        List<PostFavorite> favorites = favResult.getRecords();

        List<Map<String, Object>> records = favorites.stream().map(fav -> {
            Post post = this.getById(fav.getPostId());
            if (post == null) return null;

            Map<String, Object> map = new HashMap<>();
            map.put("id", post.getId());
            map.put("userId", post.getUserId());
            map.put("type", post.getType());
            map.put("content", post.getContent());
            map.put("likeCount", post.getLikeCount());
            map.put("commentCount", post.getCommentCount());
            map.put("favoriteCount", post.getFavoriteCount());
            map.put("createdAt", post.getCreatedAt());
            map.put("favoritedAt", fav.getCreatedAt());
            map.put("isLiked", false);
            map.put("isFavorited", true);
            map.put("images", parseImages(post.getImages()));

            User user = userMapper.selectById(post.getUserId());
            if (user != null) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("username", user.getNickname());
                userInfo.put("avatar", user.getAvatar());
                map.put("user", userInfo);
            }

            if (post.getItemId() != null) {
                Item item = itemMapper.selectById(post.getItemId());
                if (item != null) {
                    Map<String, Object> itemInfo = new HashMap<>();
                    itemInfo.put("id", item.getId());
                    itemInfo.put("title", item.getTitle());
                    itemInfo.put("price", item.getPrice());
                    itemInfo.put("coverImage", item.getCoverImage());
                    map.put("item", itemInfo);
                }
            }

            return map;
        }).filter(m -> m != null).collect(Collectors.toList());

        PageVO<Map<String, Object>> pageVO = new PageVO<>();
        pageVO.setRecords(records);
        pageVO.setTotal(favResult.getTotal());
        pageVO.setPage(page);
        pageVO.setSize(size);
        return pageVO;
    }
}
