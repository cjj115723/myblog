package com.seagull.myblog.service.impl;

import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.exceptions.ClientException;
import com.seagull.myblog.component.RandomNum;
import com.seagull.myblog.mapper.RoleMapper;
import com.seagull.myblog.mapper.UserMapper;
import com.seagull.myblog.model.User;
import com.seagull.myblog.service.RegisterService;
import com.seagull.myblog.service.redis.RedisService;
import com.seagull.myblog.utils.AliyunClientUtil;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

/**
 * @author Seagull_gby
 * @date 2019/3/23 11:24
 * Description: 注册接口实现
 */
@Service
public class RegisterServiceImpl implements RegisterService {

    private static final int ROLE_USER = 2;

    @Autowired
    private RandomNum randomNum;

    @Autowired
    private RedisService redisService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Override
    public JSONObject sendPhoneCode(String phone, int type) throws ClientException {
        JSONObject spc = new JSONObject();

        int sixRandow = randomNum.getSixRandomNum();
        redisService.setAndTimeOut(phone, sixRandow, 180);

        SendSmsResponse sendSmsResponse = AliyunClientUtil.sendSms(phone, sixRandow, type);

        if(sendSmsResponse.getCode() != null && sendSmsResponse.getCode().equals("OK")) {
            spc.put("code", 200);
            spc.put("msg", "success");
        } else {
            spc.put("code", sendSmsResponse.getCode());
            spc.put("msg", sendSmsResponse.getMessage());
        }

        return spc;
    }

    @Override
    public JSONObject nameRepeatCheck(String name) {
        JSONObject nrc = new JSONObject();
        nrc.put("code", 200);

        List<String> allNames = userMapper.queryAllName();
        for (String n : allNames) {
            if(n.equals(name)) {
                nrc.put("msg", "有人跟您的昵称重复了鸭~");
                return nrc;
            }
        }

        nrc.put("msg", "success");
        return nrc;
    }

    @Override
    public JSONObject phoneRepeatCheck(String phone) {
        JSONObject prc = new JSONObject();
        prc.put("code", 200);

        List<String> allPhones = userMapper.queryAllPhone();
        for (String p : allPhones) {
            if(p.equals(phone)) {
                prc.put("msg", "竟然拿注册过的手机号糊弄我！哼！");
                return prc;
            }
        }

        prc.put("msg", "success");
        return prc;
    }

    @Override
    public JSONObject phoneExistCheck(String phone) {
        JSONObject prc = new JSONObject();
        prc.put("code", 200);

        List<String> allPhones = userMapper.queryAllPhone();
        int flag = 0;
        for (String p : allPhones) {
            if(p.equals(phone)) {
                flag = 1;
                break;
            }
        }

        if(flag == 0) {
            prc.put("msg", "这个手机号没有注册过鸭！");
            return prc;
        }

        prc.put("msg", "success");
        return prc;
    }

    @Override
    public JSONObject captchaCheck(String phone, int captcha) {
        JSONObject cc = new JSONObject();
        cc.put("code", 200);

        if (redisService.hasKey(phone)) {
            int saveCaptcha = (int) redisService.get(phone);
            if (saveCaptcha != captcha) {
                cc.put("msg", "发给你的明明不是介个验证码！");
                return cc;
            }
        } else {
            cc.put("msg", "瞎输一个可是糊弄不过我的鸭！");
            return cc;
        }

        cc.put("msg", "success");
        return cc;
    }

    @Override
    public void insertUser(User user) {
        /* 用UUID作为用户唯一ID存储 */
        String userId = UUID.randomUUID().toString().replace("-", "");
        user.setId(userId);

        /* 密码加密 */
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        user.setPassword(encoder.encode(user.getPassword()));

        userMapper.insertUser(user);
        roleMapper.insertUserRole(userId, ROLE_USER);

    }

}
