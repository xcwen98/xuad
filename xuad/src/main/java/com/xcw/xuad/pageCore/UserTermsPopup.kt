package com.xcw.xuad.pageCore

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.collections.firstOrNull
import kotlin.let

/**
 * 用户协议弹窗组件
 * 按照设计要求实现协议弹窗UI
 *
 * @param themeColor 主题色
 * @param appName 应用名称
 * @param onAgree 点击同意的回调
 * @param onDisagree 点击不同意的回调
 * @param onProtocolClick 点击协议链接的回调 (协议名称) -> Unit
 * @param onDismiss 关闭弹窗的回调
 */
@Composable
fun UserTermsPopup(
    themeColor: Color,
    appName: String,
    onAgree: () -> Unit,
    onDisagree: () -> Unit,
    onProtocolClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = { /* 不允许点击外部关闭 */ }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题：服务协议（采用ThemeColor颜色）
                Text(
                    text = "服务协议",
                    color = themeColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 欢迎使用我买的应用！（黑色加粗）
                Text(
                    text = "欢迎使用${appName}！",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 为了更好地为您提供服务，请您仔细阅读并同意以下协议：
                Text(
                    text = "为了更好地为您提供服务，请您仔细阅读并同意以下协议：",
                    color = Color.Black,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Start,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 《用户协议》和《隐私协议》（两个协议都是可点击的，颜色都是ThemeColor）
                val annotatedText = buildAnnotatedString {
                    pushStringAnnotation(tag = "user_agreement", annotation = "user_agreement")
                    withStyle(style = SpanStyle(color = themeColor, fontWeight = FontWeight.Medium)) {
                        append("《用户协议》")
                    }
                    pop()
                    
                    append("和")
                    
                    pushStringAnnotation(tag = "privacy_agreement", annotation = "privacy_agreement")
                    withStyle(style = SpanStyle(color = themeColor, fontWeight = FontWeight.Medium)) {
                        append("《隐私协议》")
                    }
                    pop()
                }
                
                ClickableText(
                    text = annotatedText,
                    onClick = { offset ->
                        annotatedText.getStringAnnotations(
                            tag = "user_agreement",
                            start = offset,
                            end = offset
                        ).firstOrNull()?.let {
                            onProtocolClick("用户协议")
                        }
                        
                        annotatedText.getStringAnnotations(
                            tag = "privacy_agreement",
                            start = offset,
                            end = offset
                        ).firstOrNull()?.let {
                            onProtocolClick("隐私协议")
                        }
                    },
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 如果您不同意以上条款，请点击"不同意"退出应用。（灰色字）
                Text(
                    text = "如果您不同意以上条款，请点击\"不同意\"退出应用。",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Start,
                    lineHeight = 16.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 按钮行：不同意（灰色边框，灰色字，透明背景）---同意（白字，ThemeColor颜色背景）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 不同意按钮
                    OutlinedButton(
                        onClick = onDisagree,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.Gray
                        ),
                        border = BorderStroke(1.dp, Color.Gray),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "不同意",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // 同意按钮
                    Button(
                        onClick = onAgree,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "同意",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}