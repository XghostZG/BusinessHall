<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup>
// App.vue 根组件
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
  user-select: none;
}

body {
  font-family: Arial, sans-serif;
  background-color: #f5f5f5;
}

#app {
  min-height: 100vh;
}

/* 确保输入框、文本区域等可编辑元素仍然可以选择文本 */
input, textarea, select, .el-input, .el-input__inner, .el-textarea__inner {
  -webkit-user-select: text;
  -moz-user-select: text;
  -ms-user-select: text;
  user-select: text;
}
</style>