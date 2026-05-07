<template>
  <div class="error-page">
    <h1>404</h1>
    <p>抱歉，您访问的页面不存在</p>
    <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
  </div>
</template>

<style scoped>
.error-page {
  text-align: center;
  padding: 100px 0;
}
.error-page h1 {
  font-size: 72px;
  color: #409EFF;
}
</style>