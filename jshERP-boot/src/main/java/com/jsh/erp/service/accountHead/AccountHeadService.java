package com.jsh.erp.service.accountHead;

import com.alibaba.fastjson.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.datasource.entities.AccountHead;
import com.jsh.erp.datasource.entities.AccountHeadExample;
import com.jsh.erp.datasource.entities.AccountHeadVo4ListEx;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.AccountHeadMapper;
import com.jsh.erp.datasource.mappers.AccountHeadMapperEx;
import com.jsh.erp.datasource.mappers.AccountItemMapperEx;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.service.accountItem.AccountItemService;
import com.jsh.erp.service.log.LogService;
import com.jsh.erp.service.orgaUserRel.OrgaUserRelService;
import com.jsh.erp.service.user.UserService;
import com.jsh.erp.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.jsh.erp.utils.Tools.getCenternTime;

@Service
public class AccountHeadService {
    private Logger logger = LoggerFactory.getLogger(AccountHeadService.class);
    @Resource
    private AccountHeadMapper accountHeadMapper;
    @Resource
    private AccountHeadMapperEx accountHeadMapperEx;
    @Resource
    private OrgaUserRelService orgaUserRelService;
    @Resource
    private AccountItemService accountItemService;
    @Resource
    private UserService userService;
    @Resource
    private LogService logService;
    @Resource
    private AccountItemMapperEx accountItemMapperEx;

    public AccountHead getAccountHead(long id) throws Exception {
        AccountHead result=null;
        try{
            result=accountHeadMapper.selectByPrimaryKey(id);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public List<AccountHead> getAccountHeadListByIds(String ids)throws Exception {
        List<Long> idList = StringUtil.strToLongList(ids);
        List<AccountHead> list = new ArrayList<>();
        try{
            AccountHeadExample example = new AccountHeadExample();
            example.createCriteria().andIdIn(idList);
            list = accountHeadMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<AccountHead> getAccountHead() throws Exception{
        AccountHeadExample example = new AccountHeadExample();
        List<AccountHead> list=null;
        try{
            list=accountHeadMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<AccountHeadVo4ListEx> select(String type, String roleType, String billNo, String beginTime, String endTime, int offset, int rows) throws Exception{
        List<AccountHeadVo4ListEx> resList = new ArrayList<AccountHeadVo4ListEx>();
        List<AccountHeadVo4ListEx> list=null;
        try{
            String [] creatorArray = getCreatorArray(roleType);
            list = accountHeadMapperEx.selectByConditionAccountHead(type, creatorArray, billNo, beginTime, endTime, offset, rows);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        if (null != list) {
            for (AccountHeadVo4ListEx ah : list) {
                if(ah.getChangeAmount() != null) {
                    ah.setChangeAmount(ah.getChangeAmount().abs());
                }
                if(ah.getTotalPrice() != null) {
                    ah.setTotalPrice(ah.getTotalPrice().abs());
                }
                ah.setBillTimeStr(getCenternTime(ah.getBillTime()));
                resList.add(ah);
            }
        }
        return resList;
    }

    public Long countAccountHead(String type, String roleType, String billNo, String beginTime, String endTime) throws Exception{
        Long result=null;
        try{
            String [] creatorArray = getCreatorArray(roleType);
            result = accountHeadMapperEx.countsByAccountHead(type, creatorArray, billNo, beginTime, endTime);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    /**
     * 根据角色类型获取操作员数组
     * @param roleType
     * @return
     * @throws Exception
     */
    private String[] getCreatorArray(String roleType) throws Exception {
        String creator = "";
        User user = userService.getCurrentUser();
        if(BusinessConstants.ROLE_TYPE_PRIVATE.equals(roleType)) {
            creator = user.getId().toString();
        } else if(BusinessConstants.ROLE_TYPE_THIS_ORG.equals(roleType)) {
            creator = orgaUserRelService.getUserIdListByUserId(user.getId());
        }
        String [] creatorArray=null;
        if(StringUtil.isNotEmpty(creator)){
            creatorArray = creator.split(",");
        }
        return creatorArray;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertAccountHead(JSONObject obj, HttpServletRequest request) throws Exception{
        AccountHead accountHead = JSONObject.parseObject(obj.toJSONString(), AccountHead.class);
        int result=0;
        try{
            User userInfo=userService.getCurrentUser();
            accountHead.setCreator(userInfo==null?null:userInfo.getId());
            result = accountHeadMapper.insertSelective(accountHead);
            logService.insertLog("财务",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(accountHead.getBillNo()).toString(), request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateAccountHead(JSONObject obj, HttpServletRequest request)throws Exception {
        AccountHead accountHead = JSONObject.parseObject(obj.toJSONString(), AccountHead.class);
        int result=0;
        try{
            result = accountHeadMapper.updateByPrimaryKeySelective(accountHead);
            logService.insertLog("财务",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(accountHead.getBillNo()).toString(), request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteAccountHead(Long id, HttpServletRequest request)throws Exception {
        return batchDeleteAccountHeadByIds(id.toString());
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteAccountHead(String ids, HttpServletRequest request)throws Exception {
        return batchDeleteAccountHeadByIds(ids);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteAccountHeadByIds(String ids)throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(BusinessConstants.LOG_OPERATION_TYPE_DELETE);
        List<AccountHead> list = getAccountHeadListByIds(ids);
        for(AccountHead accountHead: list){
            sb.append("[").append(accountHead.getBillNo()).append("]");
        }
        logService.insertLog("财务", sb.toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        User userInfo=userService.getCurrentUser();
        String [] idArray=ids.split(",");
        int result = 0;
        try{
            //删除主表
            result = accountItemMapperEx.batchDeleteAccountItemByHeadIds(new Date(),userInfo==null?null:userInfo.getId(),idArray);
            //删除子表
            result = accountHeadMapperEx.batchDeleteAccountHeadByIds(new Date(),userInfo==null?null:userInfo.getId(),idArray);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public int checkIsNameExist(Long id, String name)throws Exception {
        AccountHeadExample example = new AccountHeadExample();
        example.createCriteria().andIdNotEqualTo(id).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<AccountHead> list = null;
        try{
            list = accountHeadMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list==null?0:list.size();
    }

    public void addAccountHeadAndDetail(String beanJson, String rows, HttpServletRequest request) throws Exception {
        AccountHead accountHead = JSONObject.parseObject(beanJson, AccountHead.class);
        User userInfo=userService.getCurrentUser();
        accountHead.setCreator(userInfo==null?null:userInfo.getId());
        accountHeadMapper.insertSelective(accountHead);
        //根据单据编号查询单据id
        AccountHeadExample dhExample = new AccountHeadExample();
        dhExample.createCriteria().andBillNoEqualTo(accountHead.getBillNo()).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<AccountHead> list = accountHeadMapper.selectByExample(dhExample);
        if(list!=null) {
            Long headId = list.get(0).getId();
            String type = list.get(0).getType();
            /**处理单据子表信息*/
            accountItemService.saveDetials(rows, headId, type, request);
        }
        logService.insertLog("财务单据",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(accountHead.getBillNo()).toString(), request);
    }

    public void updateAccountHeadAndDetail(String beanJson, String rows, HttpServletRequest request) throws Exception {
        AccountHead accountHead = JSONObject.parseObject(beanJson, AccountHead.class);
        accountHeadMapper.updateByPrimaryKeySelective(accountHead);
        //根据单据编号查询单据id
        AccountHeadExample dhExample = new AccountHeadExample();
        dhExample.createCriteria().andBillNoEqualTo(accountHead.getBillNo()).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<AccountHead> list = accountHeadMapper.selectByExample(dhExample);
        if(list!=null) {
            Long headId = list.get(0).getId();
            String type = list.get(0).getType();
            /**处理单据子表信息*/
            accountItemService.saveDetials(rows, headId, type, request);
        }
        logService.insertLog("财务单据",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(accountHead.getBillNo()).toString(), request);
    }

    public BigDecimal findAllMoney(Integer supplierId, String type, String mode, String endTime) {
        String modeName = "";
        if (mode.equals("实际")) {
            modeName = "change_amount";
        } else if (mode.equals("合计")) {
            modeName = "total_price";
        }
        BigDecimal result = null;
        try{
            result = accountHeadMapperEx.findAllMoney(supplierId, type, modeName, endTime);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    /**
     * 统计总金额
     * @param getS
     * @param type
     * @param mode 合计或者金额
     * @param endTime
     * @return
     */
    public BigDecimal allMoney(String getS, String type, String mode, String endTime) {
        BigDecimal allMoney = BigDecimal.ZERO;
        try {
            Integer supplierId = Integer.valueOf(getS);
            BigDecimal sum = findAllMoney(supplierId, type, mode, endTime);
            if(sum != null) {
                allMoney = sum;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //返回正数，如果负数也转为正数
        if ((allMoney.compareTo(BigDecimal.ZERO))==-1) {
            allMoney = allMoney.abs();
        }
        return allMoney;
    }

    /**
     * 查询单位的累计应收和累计应付，收预付款不计入此处
     * @param supplierId
     * @param endTime
     * @param supType
     * @return
     */
    public BigDecimal findTotalPay(Integer supplierId, String endTime, String supType) {
        BigDecimal sum = BigDecimal.ZERO;
        String getS = supplierId.toString();
        int i = 1;
        if (("customer").equals(supType)) { //客户
            i = 1;
        } else if (("vendor").equals(supType)) { //供应商
            i = -1;
        }
        //收付款部分
        sum = sum.add((allMoney(getS, "付款", "合计",endTime).add(allMoney(getS, "付款", "实际",endTime))).multiply(new BigDecimal(i)));
        sum = sum.subtract((allMoney(getS, "收款", "合计",endTime).add(allMoney(getS, "收款", "实际",endTime))).multiply(new BigDecimal(i)));
        sum = sum.add((allMoney(getS, "收入", "合计",endTime).subtract(allMoney(getS, "收入", "实际",endTime))).multiply(new BigDecimal(i)));
        sum = sum.subtract((allMoney(getS, "支出", "合计",endTime).subtract(allMoney(getS, "支出", "实际",endTime))).multiply(new BigDecimal(i)));
        return sum;
    }

    public List<AccountHeadVo4ListEx> getDetailByNumber(String billNo)throws Exception {
        List<AccountHeadVo4ListEx> resList = new ArrayList<AccountHeadVo4ListEx>();
        List<AccountHeadVo4ListEx> list = null;
        try{
            list = accountHeadMapperEx.getDetailByNumber(billNo);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        if (null != list) {
            for (AccountHeadVo4ListEx ah : list) {
                if(ah.getChangeAmount() != null) {
                    ah.setChangeAmount(ah.getChangeAmount().abs());
                }
                if(ah.getTotalPrice() != null) {
                    ah.setTotalPrice(ah.getTotalPrice().abs());
                }
                ah.setBillTimeStr(getCenternTime(ah.getBillTime()));
                resList.add(ah);
            }
        }
        return resList;
    }
}
