/*
 * 
 * LegendShop 多用户商城系统
 * 
 *  版权所有,并保留所有权利。
 * 
 */
package com.legendshop.business.service.timer.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import com.legendshop.business.common.CommonServiceUtil;
import com.legendshop.business.common.Constants;
import com.legendshop.business.common.OrderStatusEnum;
import com.legendshop.business.common.PayTypeEnum;
import com.legendshop.business.common.SubForm;
import com.legendshop.business.common.SubStatusEnum;
import com.legendshop.business.dao.BasketDao;
import com.legendshop.business.dao.SubDao;
import com.legendshop.business.service.PayTypeService;
import com.legendshop.business.service.timer.SubService;
import com.legendshop.model.entity.Basket;
import com.legendshop.model.entity.PayType;
import com.legendshop.model.entity.Sub;
import com.legendshop.util.AppUtils;

/**
 * 订单服务实现.
 */
public class SubServiceImpl implements SubService {
	
	/** The log. */
	private static Logger log = LoggerFactory.getLogger(SubServiceImpl.class);
	
	/** The sub dao. */
	private SubDao subDao;
	
	/** The basket dao. */
	private BasketDao basketDao;
	
	/** The pay type service. */
	private PayTypeService payTypeService;

	/** The commit inteval. */
	private Integer commitInteval = 100;
	
	/** The expire date. */
	private Integer expireDate = -30;

	/**
	 * 结束超时不确认收货的订单.
	 */
	@Override
	public void finishUnAcklodge() {
		Date date = getDate(expireDate);
		log.debug("finishUnAcklodge,date = {}", date);
		boolean haveValue = true;
		while (haveValue) {
			List<Sub> list = subDao.getUnAcklodgeSub(commitInteval, date);
			log.debug("finishUnAcklodge,list = {}", list);
			if (AppUtils.isBlank(list)) {
				haveValue = false;
			} else {
				for (Sub sub : list) {
					subDao.saveSubHistory(sub, SubStatusEnum.ORDER_OVER_TIME.value());
					sub.setStatus(OrderStatusEnum.SUCCESS.value());
					sub.setSubCheck(Constants.TRUE_INDICATOR);
					sub.setUpdateDate(new Date());
					subDao.update(sub);
				}
				subDao.flush();
			}

		}

	}

	/**
	 * 结束超时不付费的订单.
	 */
	@Override
	public void finishUnPay() {
		Date date = getDate(expireDate);
		log.debug("finishUnPay,date = {}", date);
		boolean haveValue = true;
		while (haveValue) {
			List<Sub> list = subDao.getFinishUnPay(commitInteval, date);
			log.debug("finishUnPay,list = {}", list);
			if (AppUtils.isBlank(list)) {
				haveValue = false;
			} else {
				for (Sub sub : list) {
					subDao.saveSubHistory(sub, SubStatusEnum.ORDER_OVER_TIME.value());
					sub.setStatus(OrderStatusEnum.CLOSE.value());
					sub.setSubCheck(Constants.TRUE_INDICATOR);
					sub.setUpdateDate(new Date());
					subDao.update(sub);
				}
				subDao.flush();
			}

		}
	}

	/**
	 * 移除已经过期的购物车，保留30天.
	 */
	@Override
	public void removeOverTimeBasket() {
		Date date = getDate(expireDate);
		subDao.deleteOverTimeBasket(date);

	}

	/**
	 * 得到跟现在若干天时间，如果为负数则向前推.
	 * 
	 * @param days
	 *            the days
	 * @return the date
	 */
	private Date getDate(int days) {
		Date myDate = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(myDate);
		cal.add(Calendar.DATE, days);
		return cal.getTime();
	}

	/**
	 * Sets the sub dao.
	 * 
	 * @param subDao
	 *            the new sub dao
	 */
	@Required
	public void setSubDao(SubDao subDao) {
		this.subDao = subDao;
	}

	/**
	 * Sets the commit inteval.
	 * 
	 * @param commitInteval
	 *            the new commit inteval
	 */
	public void setCommitInteval(Integer commitInteval) {
		this.commitInteval = commitInteval;
	}

	/**
	 * Sets the expire date.
	 * 
	 * @param expireDate
	 *            the new expire date
	 */
	public void setExpireDate(Integer expireDate) {
		this.expireDate = expireDate;
	}

	/**
	 * 保存订单 返回订单列表.
	 * 
	 * @param form
	 *            the form
	 * @return the list
	 */
	@Override
	public List<Sub> saveSub(SubForm form) {
		List<Sub> subList = new ArrayList<Sub>();

		// 得到用户购物车里所有的商品，每一个商城生成一个订单
		// TODO basket增加sub_id, sub删除basket_id
		Map<String, List<Basket>> basketMap = basketDao.getBasketByuserNameGroupByShopName(form.getUserName());
		if (!AppUtils.isBlank(basketMap)) {
			for (List<Basket> baskets : basketMap.values()) {
				String subNember = CommonServiceUtil.getSubNember(form.getUserName());
				String basketId = "";
				String prodName = "";
				for (Basket backet : baskets) {
					basketId = basketId + backet.getBasketId() + ",";
					prodName = prodName + backet.getProdName() + ",";
					Basket basket = basketDao.getBasketById(backet.getBasketId());
					if (basket != null) {
						basket.setSubNumber(subNember);
						basket.setBasketCheck(Constants.TRUE_INDICATOR);
						basket.setLastUpdateDate(new Date());
						basketDao.updateBasket(basket);
						basketDao.saveStocks(basket.getProdId(), basket.getBasketCount());
					}
				}
				if (!AppUtils.isBlank(basketId)) {
					basketId = basketId.substring(0, basketId.length() - 1);
				}
				if (!AppUtils.isBlank(prodName)) {
					if (prodName.length() <= 1024) {
						prodName = prodName.substring(0, prodName.length() - 1);
					} else {
						prodName = prodName.substring(0, 1021) + "...";
					}

				}
				Sub bo = makeSub(form);
				bo.setBasketId(basketId);
				bo.setSubNumber(subNember);
				bo.setTotal(CommonServiceUtil.calculateTotalCash(baskets));
				bo.setActualTotal(bo.getTotal());
				bo.setShopName(baskets.get(0).getShopName()); // 在同一组的商品中都是同一个商城的，所以取第一个商品所属的商城即可。
				bo.setStatus(OrderStatusEnum.UNPAY.value());// 还没有开始付款
				List<PayType> payTypeList = payTypeService.getPayTypeList(baskets.get(0).getShopName());
				if (payTypeList != null && payTypeList.size() == 1) {// 当该卖家只是支持货到付款则直接设置付款方式
					PayType payType = payTypeList.get(0);
					if (payType.getPayTypeId().equals(PayTypeEnum.PAY_AT_GOODS_ARRIVED.value())) {
						bo.setPayId(payType.getPayId());
						bo.setPayDate(new Date());
						bo.setPayTypeId(payType.getPayTypeId());
						bo.setPayTypeName(payType.getPayTypeName());
					}
				}
				bo.setProdName(prodName);
				subDao.saveSub(bo);
				bo.setBasket(baskets);
				bo.setPayType(payTypeList);
				subList.add(bo);
			}
		}

		return subList;

	}

	// 初始化订单
	/**
	 * Make sub.
	 * 
	 * @param form
	 *            the form
	 * @return the sub
	 */
	private Sub makeSub(SubForm form) {
		Sub sub = new Sub();
		sub.setUserName(form.getUserName());
		// sub.setSubNumber(form.getSubNember());
		sub.setSubDate(new Date());
		// sub.setTotal(form.getTotal());
		sub.setSubTel(form.getUserTel());
		sub.setSubPost(form.getUserPostcode());
		sub.setSubMail(form.getUserMail());
		sub.setSubAdds(form.getUserAdds());
		sub.setPayId(form.getPayType());
		sub.setOther(form.getOther());
		sub.setSubCheck(Constants.FALSE_INDICATOR);
		sub.setOrderName(form.getOrderName());
		return sub;
	}

	/**
	 * Sets the basket dao.
	 * 
	 * @param basketDao
	 *            the new basket dao
	 */
	@Required
	public void setBasketDao(BasketDao basketDaoImpl) {
		this.basketDao = basketDaoImpl;
	}

	/**
	 * Sets the pay type service.
	 * 
	 * @param payTypeService
	 *            the new pay type service
	 */
	@Required
	public void setPayTypeService(PayTypeService payTypeService) {
		this.payTypeService = payTypeService;
	}

}
