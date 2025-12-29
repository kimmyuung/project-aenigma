import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './ErrorStyles.css';

interface ErrorFallbackProps {
    error: Error | null;
    errorInfo: React.ErrorInfo | null;
    onReset?: () => void;
}

/**
 * 에러 발생 시 표시되는 폴백 UI
 */
export function ErrorFallback({ error, errorInfo, onReset }: ErrorFallbackProps) {
    const [showDetails, setShowDetails] = useState(false);
    const navigate = useNavigate();

    const handleGoHome = () => {
        onReset?.();
        navigate('/');
    };

    const handleRefresh = () => {
        onReset?.();
        window.location.reload();
    };

    const getErrorCode = (error: Error | null): string => {
        if (!error) return 'UNKNOWN_ERROR';

        // 에러 메시지에서 코드 추출 시도
        if (error.message.includes('게임')) return 'GAME_ERROR';
        if (error.message.includes('방')) return 'ROOM_ERROR';
        if (error.message.includes('인증') || error.message.includes('로그인')) return 'AUTH_ERROR';
        if (error.message.includes('네트워크') || error.message.includes('fetch')) return 'NETWORK_ERROR';

        return 'RENDER_ERROR';
    };

    const getErrorMessage = (error: Error | null): string => {
        if (!error) return '알 수 없는 오류가 발생했습니다.';

        // 사용자 친화적 메시지로 변환
        if (error.message.includes('fetch')) {
            return '서버와 연결할 수 없습니다. 인터넷 연결을 확인해주세요.';
        }

        return error.message || '문제가 발생했습니다.';
    };

    return (
        <div className="error-fallback">
            <div className="error-fallback__container">
                <div className="error-fallback__icon">⚠️</div>

                <h1 className="error-fallback__title">문제가 발생했습니다</h1>

                <div className="error-fallback__code">
                    에러 코드: {getErrorCode(error)}
                </div>

                <p className="error-fallback__message">
                    {getErrorMessage(error)}
                </p>

                <div className="error-fallback__actions">
                    <button
                        className="btn btn-primary btn-ripple"
                        onClick={handleRefresh}
                    >
                        🔄 새로고침
                    </button>
                    <button
                        className="btn btn-secondary"
                        onClick={handleGoHome}
                    >
                        🏠 홈으로
                    </button>
                </div>

                <button
                    className="error-fallback__details-toggle"
                    onClick={() => setShowDetails(!showDetails)}
                >
                    {showDetails ? '▲ 상세 정보 숨기기' : '▼ 상세 정보 보기'}
                </button>

                {showDetails && (
                    <div className="error-fallback__details">
                        <h4>에러 상세</h4>
                        <pre className="error-fallback__stack">
                            {error?.stack || '스택 정보 없음'}
                        </pre>

                        {errorInfo?.componentStack && (
                            <>
                                <h4>컴포넌트 스택</h4>
                                <pre className="error-fallback__stack">
                                    {errorInfo.componentStack}
                                </pre>
                            </>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}

export default ErrorFallback;
