@echo off
echo ========================================
echo  山姆会员管理系统 - 一键启动
echo ========================================
echo.

set JAVA_HOME=C:\Users\赵\.jdks\ms-17.0.18
set PATH=%JAVA_HOME%\bin;%PATH%

rem 优先使用 Maven Wrapper 已下载的发行版，缺失时回退到 mvnw.cmd
set MVN_CMD=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.14\db91789b\bin\mvn.cmd
if not exist "%MVN_CMD%" set MVN_CMD=mvnw.cmd

echo [1/2] 启动后端 (Spring Boot :8080)...
start "Sam-Backend" cmd /c "cd /d %~dp0 && %MVN_CMD% spring-boot:run -DskipTests"

echo [2/2] 启动前端 (Vite :5173)...
start "Sam-Frontend" cmd /c "cd /d %~dp0frontend && npm run dev"

echo.
echo 等待服务启动...
timeout /t 8 /nobreak >nul

echo 打开浏览器 http://localhost:5173
start http://localhost:5173

echo.
echo ========================================
echo  启动完成！
echo  前端: http://localhost:5173
echo  后端: http://localhost:8080
echo  登录: admin / 123456
echo ========================================
pause
