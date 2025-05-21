package com.yu.yupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yu.yupicturebackend.constant.UserConstant;
import com.yu.yupicturebackend.exception.BusinessException;
import com.yu.yupicturebackend.exception.ErrorCode;
import com.yu.yupicturebackend.exception.ThrowUtils;
import com.yu.yupicturebackend.manager.FileManager;
import com.yu.yupicturebackend.manager.upload.FilePictureUpload;
import com.yu.yupicturebackend.manager.upload.PictureUploadTemplate;
import com.yu.yupicturebackend.manager.upload.UrlPictureUpload;
import com.yu.yupicturebackend.model.dto.file.UploadPictureResult;
import com.yu.yupicturebackend.model.dto.picture.PictureQueryDTO;
import com.yu.yupicturebackend.model.dto.picture.PictureReviewDTO;
import com.yu.yupicturebackend.model.dto.picture.PictureUploadDTO;
import com.yu.yupicturebackend.model.entity.Picture;
import com.yu.yupicturebackend.model.entity.User;
import com.yu.yupicturebackend.model.enums.PictureReviewEnum;
import com.yu.yupicturebackend.model.vo.PictureVO;
import com.yu.yupicturebackend.model.vo.UserVO;
import com.yu.yupicturebackend.service.PictureService;
import com.yu.yupicturebackend.mapper.PictureMapper;
import com.yu.yupicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author liany
 * @description 针对表【picture(图片表)】的数据库操作Service实现
 * @createDate 2025-05-09 23:29:20
 */
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Resource
    private FileManager fileManager;

    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    /**
     * 获取查询图片的包装对象
     *
     * @param pictureQueryDTO 用户查询请求封装类
     * @return 返回 QueryWrapper 对象
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryDTO pictureQueryDTO) {
        ThrowUtils.throwIf(pictureQueryDTO == null, ErrorCode.PARAMS_ERROR, "请求参数为空");

        Long id = pictureQueryDTO.getId();
        String name = pictureQueryDTO.getName();
        String introduction = pictureQueryDTO.getIntroduction();
        String category = pictureQueryDTO.getCategory();
        List<String> tags = pictureQueryDTO.getTags();
        Long picSize = pictureQueryDTO.getPicSize();
        Integer picWidth = pictureQueryDTO.getPicWidth();
        Integer picHeight = pictureQueryDTO.getPicHeight();
        Double picScale = pictureQueryDTO.getPicScale();
        String picFormat = pictureQueryDTO.getPicFormat();
        Long userId = pictureQueryDTO.getUserId();
        String searchText = pictureQueryDTO.getSearchText();
        Date createTime = pictureQueryDTO.getCreateTime();
        Date editTime = pictureQueryDTO.getEditTime();
        Date updateTime = pictureQueryDTO.getUpdateTime();
        int current = pictureQueryDTO.getCurrent();
        int size = pictureQueryDTO.getSize();
        String sortField = pictureQueryDTO.getSortField();
        String sortOrder = pictureQueryDTO.getSortOrder();
        Integer reviewStatus = pictureQueryDTO.getReviewStatus();
        String reviewMessage = pictureQueryDTO.getReviewMessage();
        Long reviewerId = pictureQueryDTO.getReviewerId();
        Date reviewTime = pictureQueryDTO.getReviewTime();

        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(category), "category", category);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            /* and (tags like "%\"JAVA\"%" and tags like "%\"PYTHON\"%") */
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        queryWrapper.like(ObjUtil.isNotEmpty(picSize), "pic_size", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "pic_width", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "pic_height", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "pic_scale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(picFormat), "pic_format", picFormat);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "user_id", userId);
        // 从多字段中查询
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            // and (name like "%xxx%" or introduction like "%xxx%")
            queryWrapper.and(
                    qw -> qw.like("name", searchText)
                            .or()
                            .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(createTime), "create_time", createTime);
        queryWrapper.eq(ObjUtil.isNotEmpty(editTime), "edit_time", editTime);
        queryWrapper.eq(ObjUtil.isNotEmpty(updateTime), "update_time", updateTime);

        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "review_status", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "review_message", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewer_id", reviewerId);

        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 获取图片包装类（单条）
     *
     * @param picture
     * @return
     */
    @Override
    public PictureVO getPictureVO(Picture picture) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 获取关联的用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 获取图片包装类（分页）
     *
     * @param picturePage
     * @return
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        // 分页结果为空，则返回空的分页列表
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());

        // 1、 查询关联用户信息
        Set<Long> userIdsSet = pictureVOList.stream().map(PictureVO::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdsSet)
                .stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2、填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    /**
     * 校验图片
     *
     * @param picture
     */
    @Override
    public void validatePicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改图片时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    /**
     * 填充审核参数
     *
     * @param picture   图片
     * @param loginUser 当前登录用户
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewEnum.PASS.getValue());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，创建和修改图片都要变成待审核
            picture.setReviewStatus(PictureReviewEnum.REVIEWING.getValue());
        }
    }

    /**
     * 上传图片
     *
     * @param inputSource      文件输入源
     * @param pictureUploadDTO 上传图片请求的参数封装类
     * @param loginUser        当前登录用户
     * @return 返回上传的图片封装信息
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadDTO pictureUploadDTO, User loginUser) {
        // 参数校验
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 判断是新增还是编辑
        Long pictureId = null;
        if (pictureUploadDTO != null) {
            pictureId = pictureUploadDTO.getId();
        }
        // 如果是编辑，判断文件是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 只有本人及管理员才可以编辑图片
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
        // 上传图片，得到图片信息
        // 按照用户 id，划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        // 根据 inputSource 的类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 构造要入库的图片信息
        Picture picture = new Picture();
        BeanUtils.copyProperties(uploadPictureResult, picture);
        picture.setUserId(loginUser.getId());
        // 操作数据库
        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        // 补全审核参数
        fillReviewParams(picture, loginUser);
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败，数据库操作失败");
        return PictureVO.objToVo(picture);
    }

    /**
     * 图片审核
     *
     * @param pictureReviewDTO 图片审核参数
     * @param loginUser        当前登录用户
     */
    @Override
    public void pictureReview(PictureReviewDTO pictureReviewDTO, User loginUser) {
        // 1、参数校验、已审核的图片不能变为待审核
        Long id = pictureReviewDTO.getId();
        Integer reviewStatus = pictureReviewDTO.getReviewStatus();
        PictureReviewEnum reviewStatusEnum = PictureReviewEnum.getEnumByValue(reviewStatus);
        if (id == null || reviewStatusEnum == null || PictureReviewEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2、图片不能为空
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 3、不能重复审核
        ThrowUtils.throwIf(oldPicture.getReviewStatus().equals(reviewStatus), ErrorCode.PARAMS_ERROR, "请勿重复审核");
        // 4、操作数据库
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewDTO, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean updated = this.updateById(updatePicture);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR);
    }

}




