/*
 * 
 * LegendShop 多用户商城系统
 * 
 *  版权所有,并保留所有权利。
 * 
 */
package com.legendshop.business.dao.impl;

import java.util.List;

import net.sf.ehcache.Cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.legendshop.business.common.CacheKeys;
import com.legendshop.business.dao.SortDao;
import com.legendshop.core.cache.CacheCallBack;
import com.legendshop.core.dao.impl.BaseDaoImpl;
import com.legendshop.core.dao.support.CriteriaQuery;
import com.legendshop.core.dao.support.PageSupport;
import com.legendshop.model.entity.Nsort;
import com.legendshop.model.entity.Sort;

/**
 * 产品分类Dao.
 */
@SuppressWarnings("unchecked")
public class SortDaoImpl extends BaseDaoImpl implements SortDao {
	
	/** The log. */
	private static Logger log = LoggerFactory.getLogger(SortDaoImpl.class);

	/* (non-Javadoc)
	 * @see com.legendshop.business.dao.impl.SortDao#getSort(java.lang.String, java.lang.Boolean)
	 */
	@Override
	public List<Sort> getSort(final String shopName, final Boolean loadAll) {
		log.debug("getSort, shopName = {}, loadAll = {}", shopName, loadAll);
		return (List<Sort>) getObjectFromCache(getKey(CacheKeys.SORTDAO_GETSORT, shopName, loadAll),
				new CacheCallBack<List<Sort>>() {
					@Override
					public List<Sort> doInCache(String cahceName, Cache cache) {
						List<Sort> list = findByHQL("from Sort where userName = ? order by seq", shopName);
						if (loadAll) {
							// 找出所有的Nsort
							List<Nsort> nsortList = findByHQL(
									"select n from Nsort n, Sort s where n.sortId = s.sortId and s.userName = ?",
									shopName);
							for (int i = 0; i < nsortList.size(); i++) {
								Nsort n1 = nsortList.get(i);
								for (int j = i; j < nsortList.size(); j++) {
									Nsort n2 = nsortList.get(j);
									n1.addSubSort(n2);
									n2.addSubSort(n1);
								}
							}

							for (Sort sort : list) {
								for (Nsort nsort : nsortList) {
									sort.addSubSort(nsort);
								}
							}
						}
						return list;
					}
				});
	}

	/* (non-Javadoc)
	 * @see com.legendshop.business.dao.impl.SortDao#getSort(java.lang.Long)
	 */
	@Override
	public Sort getSort(Long sortId) {
		return get(Sort.class, sortId);
	}

	/* (non-Javadoc)
	 * @see com.legendshop.business.dao.impl.SortDao#querySort(com.legendshop.core.dao.support.CriteriaQuery)
	 */
	@Override
	public PageSupport getSortByCriteria(CriteriaQuery cq) {
		return find(cq);
	}

	/* (non-Javadoc)
	 * @see com.legendshop.business.dao.impl.SortDao#querySortList(java.lang.String)
	 */
	@Override
	public List<Sort> getSortList(String userName) {
        return findByHQL("from Sort where userName = ?", new Object[] { userName });
	}

	/* (non-Javadoc)
	 * @see com.legendshop.business.dao.impl.SortDao#deleteSortById(java.lang.Long)
	 */
	@Override
	public void deleteSortById(Long id) {
	      deleteById(Sort.class, id);
	}

	/* (non-Javadoc)
	 * @see com.legendshop.business.dao.impl.SortDao#updateSort(com.legendshop.model.entity.Sort)
	 */
	@Override
	public void updateSort(Sort sort) {
        update(sort);
	}

	/* (non-Javadoc)
	 * @see com.legendshop.business.dao.impl.SortDao#saveSort(com.legendshop.model.entity.Sort)
	 */
	@Override
	public Long saveSort(Sort sort) {
		return (Long)save(sort);
	}

	/* (non-Javadoc)
	 * @see com.legendshop.business.dao.impl.SortDao#queryProductBySortId(java.lang.Long)
	 */
	@Override
	public List getProductBySortId(Long sortId) {
		return findByHQL("from Product where sortId = ?",  sortId);
	}

	/* (non-Javadoc)
	 * @see com.legendshop.business.dao.impl.SortDao#queryNsortBySortId(java.lang.Long)
	 */
	@Override
	public List getNsortBySortId(Long sortId) {
		return findByHQL("from Nsort where sortId = ?",  sortId);
	}

	/* (non-Javadoc)
	 * @see com.legendshop.business.dao.impl.SortDao#deleteSort(com.legendshop.model.entity.Sort)
	 */
	@Override
	public void deleteSort(Sort sort) {
		delete(sort);
	}

}
