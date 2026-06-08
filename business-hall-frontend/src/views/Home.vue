<template>
  <div class="home">
    <div class="hero" id="home">
      <div class="hero-background"></div>
      <div class="container">
        <div class="hero-content">
          <h1 class="hero-title">营业厅业务预约系统</h1>
          <p class="hero-subtitle">在线预约，减少排队等待，提升服务体验</p>
          <el-button type="primary" size="large" class="hero-button" @click="toAppointment">立即预约</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const toAppointment = () => {
  router.push('/appointment')
}
</script>

<style scoped>
.home {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  scroll-behavior: smooth;
  height: 100%;
  display: flex;
  flex-direction: column;
  width: 100%;
  flex: 1;
}

/* Hero 区域 */
.hero {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 100px 0;
  text-align: center;
  position: relative;
  overflow: hidden;
  transition: all 0.5s ease;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero:hover {
  background: linear-gradient(135deg, #5a67d8 0%, #805ad5 100%);
}

.hero-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 10% 20%, rgba(255, 255, 255, 0.1) 0%, transparent 20%),
    radial-gradient(circle at 80% 30%, rgba(255, 255, 255, 0.08) 0%, transparent 25%),
    radial-gradient(circle at 40% 70%, rgba(255, 255, 255, 0.12) 0%, transparent 15%);
  animation: animateBackground 20s linear infinite;
  z-index: 0;
  opacity: 0.6;
  transition: all 0.5s ease;
}

@keyframes animateBackground {
  0% {
    transform: translateX(0) translateY(0);
  }
  50% {
    transform: translateX(30px) translateY(30px);
  }
  100% {
    transform: translateX(0) translateY(0);
  }
}

.hero:hover .hero-background {
  opacity: 0.8;
  background: 
    radial-gradient(circle at 20% 30%, rgba(255, 255, 255, 0.15) 0%, transparent 25%),
    radial-gradient(circle at 70% 20%, rgba(255, 255, 255, 0.12) 0%, transparent 30%),
    radial-gradient(circle at 30% 80%, rgba(255, 255, 255, 0.18) 0%, transparent 20%);
  animation-duration: 15s;
}

.hero-content {
  position: relative;
  z-index: 1;
  transition: all 0.5s ease;
  max-width: 900px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 30px;
}

.hero:hover .hero-content {
  transform: translateY(-10px);
}

.hero-title {
  font-size: 3.5rem;
  margin-bottom: 20px;
  font-weight: 700;
  animation: heroTitleFadeIn 1.2s ease-out;
  text-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  letter-spacing: 1px;
  transition: all 0.5s ease;
  cursor: pointer;
  display: inline-block;
  line-height: 1.2;
  white-space: nowrap;
  position: relative;
}

.hero-title::after {
  content: '';
  position: absolute;
  bottom: -10px;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 3px;
  background-color: white;
  transition: width 0.5s ease;
  border-radius: 2px;
}

.hero:hover .hero-title {
  transform: scale(1.03);
  text-shadow: 0 6px 16px rgba(0, 0, 0, 0.3);
  letter-spacing: 1.5px;
  color: #f8f9fa;
}

.hero:hover .hero-title::after {
  width: 80%;
}

@keyframes heroTitleFadeIn {
  from {
    opacity: 0;
    transform: translateY(-50px) scale(0.8);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.hero-subtitle {
  font-size: 1.3rem;
  margin-bottom: 40px;
  animation: heroSubtitleFadeIn 1.2s ease-out 0.4s both;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
  cursor: pointer;
  display: inline-block;
  line-height: 1.4;
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
}

.hero:hover .hero-subtitle {
  transform: scale(1.03);
  color: #e9ecef;
  text-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

@keyframes heroSubtitleFadeIn {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.hero-button {
  animation: heroButtonFadeIn 1.2s ease-out 0.8s both;
  padding: 18px 45px;
  font-size: 1.2rem;
  border-radius: 50px;
  transition: all 0.3s ease;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 20px;
  min-width: 200px;
  text-align: center;
  white-space: nowrap;
  position: relative;
  overflow: hidden;
}

.hero-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s ease;
}

.hero-button:hover::before {
  left: 100%;
}

@keyframes heroButtonFadeIn {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.9);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.hero-button:hover {
  transform: translateY(-5px) scale(1.05);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.25);
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(5px);
}

/* 容器 */
.container {
  width: 100%;
  padding: 0 20px;
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}





/* 通用动画 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .container {
    width: 100%;
  }
  
  .hero-title {
    font-size: 3.2rem;
  }
}

@media (max-width: 768px) {
  .hero {
    padding: 180px 0 140px;
  }
  
  .hero-title {
    font-size: 2.8rem;
  }
  
  .hero-subtitle {
    font-size: 1.1rem;
  }
  
  .hero:hover .hero-title {
    transform: scale(1.02);
  }
  
  .hero:hover .hero-subtitle {
    transform: scale(1.02);
  }
  
  .navbar-logo h2 {
    font-size: 1.3rem;
  }
  
  .navbar-menu {
    display: none;
  }
  
  .user-name {
    display: none;
  }
  
  .user-profile {
    padding: 8px;
  }
  
  .footer-content {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  
  .footer-links a {
    padding-left: 0;
  }
  
  .footer-links a::before {
    display: none;
  }
  
  .footer-links a::after {
    left: 50%;
    transform: translateX(-50%);
  }
  
  .footer-contact p {
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .hero {
    padding: 160px 0 120px;
  }
  
  .hero-title {
    font-size: 2.2rem;
    white-space: normal;
  }
  
  .hero-subtitle {
    font-size: 1rem;
  }
  
  .hero:hover .hero-title {
    transform: scale(1.01);
  }
  
  .hero:hover .hero-subtitle {
    transform: scale(1.01);
  }
  
  .hero-button {
    padding: 12px 32px;
    font-size: 1.1rem;
  }
  
  .navbar-logo h2 {
    font-size: 1.1rem;
  }
  
  .footer {
    padding: 60px 0 30px;
  }
}
</style>