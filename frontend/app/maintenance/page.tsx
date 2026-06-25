import Link from "next/link";

export default function MaintenancePage() {
  return (
    <div className="mcPage">
      <div className="mcShell">
        <div className="mcMaintenanceWrap">
          <div className="mcMaintenanceBody">
            <div className="mcMaintenanceIcon">🛠</div>
            <div className="mcErrorTitle">서비스 점검 중이에요</div>
            <p className="mcErrorDesc">
              더 나은 서비스를 위해
              <br />
              시스템을 점검하고 있어요
            </p>
            <div className="mcMaintenanceInfoBox">
              <div className="mcMaintenanceInfoRow">
                <span className="mcMaintenanceInfoLabel">점검 시간</span>
                <span className="mcMaintenanceInfoValue">02:00~06:00</span>
              </div>
              <div className="mcMaintenanceInfoRow">
                <span className="mcMaintenanceInfoLabel">완료 예정</span>
                <span className="mcMaintenanceInfoValue--done">오전 6시</span>
              </div>
            </div>
            <p className="mcMaintenanceNotice">
              점검 중에는 일부 서비스 이용이
              <br />
              제한될 수 있어요
            </p>
          </div>
          <div className="mcMaintenanceFooter">
            <Link href="/" className="mcBtn mcBtnSecondary">
              공지사항 보기
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
