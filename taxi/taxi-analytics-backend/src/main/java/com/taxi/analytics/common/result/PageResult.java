package com.taxi.analytics.common.result;

import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long total;
    private List<T> list;
    private Long pageNum;
    private Long pageSize;
    
    public PageResult() {}
    
    public PageResult(Long total, List<T> list) {
        this.total = total;
        this.list = list;
    }
}